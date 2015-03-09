import java.util.ArrayList;
import java.util.List;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


public class FournisseurAgent extends Agent {
	
	public enum StrategieFournisseur { Aleatoire, Stabilite };
	
	private List<AID> consommateurs = new ArrayList<AID>(); 
	
	// constantes pour l'enregistrement du service Fournisseur
	public static final String SERVICE_TYPE = "electricity-selling";
	public static final String SERVICE_NAME = "elec-trading";
	// contenu du message de requéte
	public static final String CREDIT_REQUEST_CONTENT = "how much electricity?";
	
	private double prix_de_vente = 10;
	private double prix_produire;
	private Transporteur transporteur = MainLauncher.erdf;
	private int temps_amortissement = 12;
	private int temps_depuis_achat = -1;
	
	public List<MoisFournisseur> historique = new ArrayList<MoisFournisseur>();
	private StrategieFournisseur strat = StrategieFournisseur.Stabilite; // on programme la stratégie de notre simulation ici
	private double benefCumule = 0;
	private int numeroMois = 0;
	
	public class BillingBehaviour extends Behaviour {
		private int step = 0;
		private int repliesCnt = 0;
		private double consoTotale = 0;
		private double prodTotale = 0;
		private MessageTemplate mtHorloge = MessageTemplate.MatchContent(HorlogeAgent.BILLING_TIME_MESSAGE_CONTENT);
		private MessageTemplate mtReply = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		
		public void action() {
			switch (step)
			{
			case 0:
				// attendre déclencheur horloge
				ACLMessage call = myAgent.receive(mtHorloge);
				if (call != null) {
					// envoyer une requête à tous les agents abonnés
					// TODO: si un agent était en train de se désabonner, il sera facturé deux fois...
					for (AID a : consommateurs)
					{
						ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
						msg.addReceiver(a);
						msg.setLanguage(MainLauncher.COMMON_LANGUAGE);
						msg.setOntology(MainLauncher.COMMON_ONTOLOGY);
						msg.setContent(CREDIT_REQUEST_CONTENT);
						send(msg);
					}
					repliesCnt = 0;
					consoTotale = 0;
					prodTotale = 0;
					step++;
				}
				else
					block();
				break;
			case 1:
				// récupérer tous les messages de réponse des clients
				ACLMessage reply = myAgent.receive(mtReply);
				if (consommateurs.size() == 0) { step++; return; }
				if (reply != null) {
					String[] chiffres = reply.getContent().split(ConsommateurAgent.SEPARATOR);
					double consommation = Double.parseDouble(chiffres[0]);
					double production = Double.parseDouble(chiffres[1]);
					consoTotale += consommation;
					prodTotale += production;
					repliesCnt++;
					if (repliesCnt >= consommateurs.size()) {
						// We received all replies
						step++;
					}
				}
				else
					block();
				break;
			case 2:
				double benef, depenses, depenses_amort, CA;
				numeroMois++;
				// le prix de production varie à chaque cycle pour le producteur
				prix_produire = MainLauncher.gaussianRandom(0.7, 0.1);
				
				// Faire le bilan du mois
				depenses = consoTotale * (transporteur.getWattPrice() + prix_produire) + 
						prodTotale * (MainLauncher.prix_rachat);
				CA = consoTotale * prix_de_vente;
				benef = CA - depenses;
				// inclure l'amortissement du transporteur dans l'estimation des dépenses
				depenses_amort = depenses;
				if (transporteur != MainLauncher.erdf && temps_depuis_achat < temps_amortissement)
					depenses_amort += MainLauncher.prix_transporteur / ((double)(consoTotale * temps_amortissement));
				
				// établir un prix de vente pour le mois prochain
				double prix_estime;
				if (consommateurs.isEmpty())
					prix_de_vente = (transporteur.getWattPrice() + prix_produire) * MainLauncher.gaussianRandom(1.5, 0.3);
				else
				{
					prix_estime = depenses_amort / consoTotale;
					prix_de_vente = prix_estime * MainLauncher.gaussianRandom(1.5, 0.2);
				}
				
				// stratégie pour le transporteur
				boolean achatTransporteur = false;
				if (transporteur == MainLauncher.erdf) {
					// quand on utilise ERDF, choisir si on prend un transporteur interne
					achatTransporteur = deciderAchatTransporteur(consommateurs.size(), benef);
					if (achatTransporteur)
					{
						transporteur = new Transporteur(0);
						temps_amortissement = (int)(MainLauncher.prix_transporteur/benef);
						benef -= MainLauncher.prix_transporteur;
						temps_depuis_achat = 0;
					}
				}
				else
					temps_depuis_achat++;
				
				MoisFournisseur ceMoisCi = new MoisFournisseur(CA, benef, prix_de_vente, 
						consommateurs.size(), transporteur != MainLauncher.erdf);
				historique.add(ceMoisCi);
				ACLMessage msgOBS = new ACLMessage(ACLMessage.INFORM);
				msgOBS.addReceiver(MainLauncher.monitor);
				msgOBS.setLanguage(MainLauncher.COMMON_LANGUAGE);
				msgOBS.setOntology(MainLauncher.COMMON_ONTOLOGY);
				if (achatTransporteur)
					msgOBS.setContent("TRANSPORTEUR ACHETE\n"+ceMoisCi.getUserMessage());
				else
					msgOBS.setContent(ceMoisCi.getUserMessage());
				send(msgOBS);
				System.out.println(getLocalName() + " a encaissé " + consoTotale);
				step=0;
				break;
			}
		}
		
		public boolean deciderAchatTransporteur(int nbClients, double benef)
		{
			// accumuler la somme des bénéfices depuis le début
			benefCumule += benef;
			switch (strat)
			{
			case Aleatoire:
				return (nbClients > 0 && Math.random()>0.8);
			case Stabilite:
				if (benefCumule > MainLauncher.prix_transporteur / 2)
					return true;
				return false;
			default:
				return false;
			}
		}

		@Override
		public boolean done() {
			return false;
		}
	}
	
	
	public class AdvertisementBehaviour extends Behaviour {
		private MessageTemplate mtRequest = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

		public void action() {
			// attendre déclencheur consommateur
			ACLMessage call = myAgent.receive(mtRequest);
			if (call != null) {
				// répondre par son prix de vente
				AID client = call.getSender();
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM_REF);
				msg.addReceiver(client);
				msg.setLanguage(MainLauncher.COMMON_LANGUAGE);
				msg.setOntology(MainLauncher.COMMON_ONTOLOGY);
				msg.setContent(Double.toString(prix_de_vente));
				send(msg);
			}
			else
				block();
		}

		@Override
		public boolean done() {
			return false;
		}
	}
	
	
	public class SubscriptionBehaviour extends Behaviour {
		private MessageTemplate mtSubscribe = MessageTemplate.MatchPerformative(ACLMessage.INFORM_IF);

		public void action() {
			// attendre qu'un client prenne une décision
			ACLMessage call = myAgent.receive(mtSubscribe);
			if (call != null) {
				AID client = call.getSender();
				if (call.getContent().compareTo(ConsommateurAgent.SUBSCRIBED_MESSAGE_CONTENT)==0)
				{
					subscribe(client);
				}
				else if (call.getContent().compareTo(ConsommateurAgent.UNSUBSCRIBED_MESSAGE_CONTENT)==0)
				{
					unsubscribe(client);
				}
			}
			else
				block();
		}

		@Override
		public boolean done() {
			return false;
		}
	}

	
	protected void setup()
	{
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType(SERVICE_TYPE);
		sd.setName(SERVICE_NAME);
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		this.addBehaviour(new BillingBehaviour());
		this.addBehaviour(new AdvertisementBehaviour());
		this.addBehaviour(new SubscriptionBehaviour());
	}

	protected void takeDown()
	{
		System.out.println();
		System.out.println(this.getLocalName() + " :\n" + MoisFournisseur.generateCSV(historique));
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}
	
	public void newCycle()
	{
		prix_produire = MainLauncher.gaussianRandom(0.8, 0.2);
	}
	
	
	public void subscribe(AID client)
	{
		consommateurs.add(client);
	}
	public void unsubscribe(AID client)
	{
		// TODO: vérifier que le client est supprimé
		/*
		for(int i = 0; i<consommateurs.size(); i++)
		{
			if (consommateurs.get(i) == client)
			{
				consommateurs.remove(i);
				return;
			}
		}*/
		consommateurs.remove(client);
	}
}
