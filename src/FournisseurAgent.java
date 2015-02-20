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
	
	private List<AID> consommateurs = new ArrayList<AID>(); 
	
	// constantes pour l'enregistrement du service Fournisseur
	public static final String SERVICE_TYPE = "electricity-selling";
	public static final String SERVICE_NAME = "elec-trading";
	// contenu du message de requête
	public static final String CREDIT_REQUEST_CONTENT = "how much electricity?";
	
	private double prix_de_vente = 1.0;
	
	
	public class BillingBehaviour extends Behaviour {
		private int step = 0;
		private int repliesCnt = 0;
		private double consoTotale = 0;
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
					step++;
					prix_de_vente = Math.random();
				}
				else
					block();
				break;
			case 1:
				// récupérer tous les messages de réponse des clients
				ACLMessage reply = myAgent.receive(mtReply);
				if (consommateurs.size() == 0) { step++; }
				if (reply != null) {
					double consommation = Double.parseDouble(reply.getContent());
					consoTotale += consommation;
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
				ACLMessage msgOBS = new ACLMessage(ACLMessage.INFORM);
				msgOBS.addReceiver(MainLauncher.monitor);
				msgOBS.setLanguage(MainLauncher.COMMON_LANGUAGE);
				msgOBS.setOntology(MainLauncher.COMMON_ONTOLOGY);
				msgOBS.setContent("encaissé " + consoTotale + "\n" + consommateurs.size() + " clients");
				send(msgOBS);
				System.out.println(getLocalName() + " a encaissé " + consoTotale);
				step=0;
				break;
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
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
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
