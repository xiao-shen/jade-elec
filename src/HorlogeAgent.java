import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

/**
 * Agent qui envoie périodiquement aux fournisseurs
 * les moments de facturation
 *
 */
public class HorlogeAgent extends Agent {
	public static final String BILLING_TIME_MESSAGE_CONTENT = "bill-time";

	protected void setup() {
		// Printout a welcome message
		System.out.println("Hello! Scheduler "+getAID().getName()+" is ready.");

		// Add a TickerBehaviour that schedules a request to seller agents every minute
		addBehaviour(new TickerBehaviour(this, 3000) {
			protected void onTick() {
				System.out.println("tick!");
				// Consulter la liste des fournisseurs
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType(FournisseurAgent.SERVICE_TYPE);
				template.addServices(sd);
				AID[] sellerAgents = null;
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					sellerAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						sellerAgents[i] = result[i].getName();
					}
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}
				
				// Envoyer le message aux agents concernés
				ACLMessage tickMsg = new ACLMessage(ACLMessage.REQUEST);
				for (AID ag : sellerAgents)
					tickMsg.addReceiver(ag);
				tickMsg.setLanguage(MainLauncher.COMMON_LANGUAGE);
				tickMsg.setOntology(MainLauncher.COMMON_ONTOLOGY);
				tickMsg.setContent(BILLING_TIME_MESSAGE_CONTENT);
				myAgent.send(tickMsg);
			}});
	}
}
