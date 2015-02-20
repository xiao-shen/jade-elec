
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;


public class Observateur extends Agent {

	public static final String SERVICE_TYPE = "electricity-observer";
	public static final String SERVICE_NAME = "elec-obs";

	JFrame fenetre;
	JTextArea[] txtFournisseur;
	JTextArea[] txtClient;
	
	public void initJFrame(int nbFournisseurs, int nbClients) {
		fenetre = new JFrame("Etat de la modélisation");
		fenetre.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		fenetre.setPreferredSize(new Dimension(600, 400));
		fenetre.setLayout(new GridLayout(2, Math.max(nbClients, nbFournisseurs)));

		txtClient = new JTextArea[nbClients];
		for (int i = 0; i < nbClients; i++)
		{
			txtClient[i] = new JTextArea("Client " + (i+1));
			txtClient[i].setEditable(false);
			fenetre.add(txtClient[i]);
		}
		
		txtFournisseur = new JTextArea[nbFournisseurs];
		for (int i = 0; i < nbFournisseurs; i++)
		{
			txtFournisseur[i] = new JTextArea("Fournisseur " + (i+1));
			txtFournisseur[i].setEditable(false);
			fenetre.add(txtFournisseur[i]);
		}

		fenetre.pack();
		fenetre.setVisible(true);
	}
	
	public void dispatchAgent(AID agent, String message)
	{
		String agentName = agent.getLocalName();
		String type = agentName.substring(0,1);
		String strNum = agentName.substring(1);
		int num = Integer.parseInt(strNum)-1;
		if (type.compareTo("F")==0)
			updateFournisseur(num, message);
		else if (type.compareTo("C")==0)
			updateClient(num, message);
		else
			System.out.println("error agent name not standard: " + agentName);
	}
	
	public void updateFournisseur(int numFournisseur, String message)
	{
		txtFournisseur[numFournisseur].setText("Fournisseur " + (numFournisseur+1) + ":\n" + message);
	}
	public void updateClient(int numClient, String message)
	{
		txtClient[numClient].setText("Client " + (numClient+1) + ":\n" + message);
	}


	public class ReceiverBehaviour extends Behaviour {
		public void action() {
			// attendre de recevoir une nouvelle information
			ACLMessage call = myAgent.receive();
			if (call != null) {
				String msg = call.getContent();
				dispatchAgent(call.getSender(), msg);
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
		MainLauncher.monitor = this.getAID();
		addBehaviour(new ReceiverBehaviour());
		initJFrame(2, 3);
	}

	protected void takeDown()
	{
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}
}
