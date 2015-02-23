
import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class ConsommateurAgent extends Agent  {
	
	public static final String SERVICE_TYPE = "elec-consumer";
	public static final String SERVICE_NAME = "elec-buying";
	public static final String PRIX_REQUEST_CONTENT = "prix?";
	public static final String UNSUBSCRIBED_MESSAGE_CONTENT = "desinscription";
	public static final String SUBSCRIBED_MESSAGE_CONTENT = "inscription";
	
	private double consoMensuel;
	
	private AID[] fournisseurAgents;
	
	private AID fournisseurCourant = null;
	
	public class SubscriptionBehaviour extends Behaviour {
		private int step = 0;
		private int repliesCnt = 0;
		private double prixMin = Double.POSITIVE_INFINITY;
		private AID fournisseurChoisi = null;
		private MessageTemplate mtReply = MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF);

		public void action() {
			switch (step)
			{
			case 0:
				// mettre à jour la liste des fournisseurs // Update the list of provider agents
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType(FournisseurAgent.SERVICE_TYPE);
				template.addServices(sd);
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					fournisseurAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						fournisseurAgents[i] = result[i].getName();
					}
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}
				// envoi requête
				for (AID a : fournisseurAgents)
				{
					ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
					msg.addReceiver(a);
					msg.setLanguage(MainLauncher.COMMON_LANGUAGE);
					msg.setOntology(MainLauncher.COMMON_ONTOLOGY);
					msg.setContent(PRIX_REQUEST_CONTENT);
					send(msg);
				}
				repliesCnt = 0;
				step++;
				break;
			case 1:
				// récupérer tous les messages de réponse des clients
				ACLMessage reply = myAgent.receive(mtReply);
				if (reply != null) {
					double prix = Double.parseDouble(reply.getContent());
					if (prix < prixMin) {
						prixMin = prix;
						fournisseurChoisi = reply.getSender();
					}
					repliesCnt++;
					if (repliesCnt >= fournisseurAgents.length) {
						// We received all replies
						step++;
					}
				}
				else
					block();
				break;
			case 2:
				if (fournisseurCourant != fournisseurChoisi && (fournisseurCourant == null || Math.random()>0.5)) {
					// se d�sabonner et ser�abonner
					if (fournisseurCourant != null) {
						// d�sabonnemment
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM_IF);
						msg.addReceiver(fournisseurCourant);
						msg.setLanguage(MainLauncher.COMMON_LANGUAGE);
						msg.setOntology(MainLauncher.COMMON_ONTOLOGY);
						msg.setContent(UNSUBSCRIBED_MESSAGE_CONTENT);
						send(msg);
					}
					// abonnement
					fournisseurCourant = fournisseurChoisi;
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM_IF);
					msg.addReceiver(fournisseurChoisi);
					msg.setLanguage(MainLauncher.COMMON_LANGUAGE);
					msg.setOntology(MainLauncher.COMMON_ONTOLOGY);
					msg.setContent(SUBSCRIBED_MESSAGE_CONTENT);
					send(msg);
					
					ACLMessage msgOBS = new ACLMessage(ACLMessage.INFORM);
					msgOBS.addReceiver(MainLauncher.monitor);
					msgOBS.setLanguage(MainLauncher.COMMON_LANGUAGE);
					msgOBS.setOntology(MainLauncher.COMMON_ONTOLOGY);
					msgOBS.setContent("abonn� � " + fournisseurChoisi.getLocalName());
					send(msgOBS);
					System.out.println("abonnement � " + fournisseurChoisi.getLocalName() + " fait");
				}
				step++;
				break;
			}
		}

		@Override
		public boolean done() {
			return (step==3);
		}
	}
	
	
	public class BillingBehaviour extends Behaviour {
		private MessageTemplate mtRequest = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

		public void action() {

			// attendre d�clencheur horloge
			ACLMessage call = myAgent.receive(mtRequest);

			if (call != null) {
				AID fournisseur = call.getSender();
				// envoyer une requ�te au fournisseur

				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				Double consom = consommation();
				msg.addReceiver(fournisseur);
				msg.setLanguage(MainLauncher.COMMON_LANGUAGE);
				msg.setOntology(MainLauncher.COMMON_ONTOLOGY);
				msg.setContent(Double.toString(consom));
				send(msg);
				
				ACLMessage msgOBS = new ACLMessage(ACLMessage.INFORM);
				msgOBS.addReceiver(MainLauncher.monitor);
				msgOBS.setLanguage(MainLauncher.COMMON_LANGUAGE);
				msgOBS.setOntology(MainLauncher.COMMON_ONTOLOGY);
				msgOBS.setContent("consomm� " + consom + "\n� " + fournisseur.getLocalName());
				send(msgOBS);
				System.out.println("done");
			}
			else
				block();
		}

		@Override
		public boolean done() {
			return false;
		}
	}
	
	private double consommation() {
		consoMensuel = MainLauncher.gaussianRandom(500, 100);
		return consoMensuel;
	}

	
	
	protected void setup() {
		// Printout a welcome message
		System.out.println("Hello! Consumer-agent "+getAID().getName()+" is ready.");
		
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
		
		addBehaviour(new TickerBehaviour(this, 10000) {
			protected void onTick() {
				myAgent.addBehaviour(new SubscriptionBehaviour());
			}
		});
		
		addBehaviour(new BillingBehaviour());
		addBehaviour(new SubscriptionBehaviour());
	}
	
	// Put agent clean-up operations here
	protected void takeDown() {
	// Printout a dismissal message
	System.out.println("Consumer-agent "+getAID().getName()+" terminating.");
	
	try {
		DFService.deregister(this);
	} catch (FIPAException e) {
		e.printStackTrace();
	}
	
	}

}

