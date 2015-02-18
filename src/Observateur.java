
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

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
	
	protected void setup()
	{
		MainLauncher.monitor = this.getAID();
		init();
	}
	
	protected void takeDown()
	{
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}
	
	public void init() {
		JFrame fenetre2 = new JFrame("Etat des fournisseurs");
		fenetre2.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		fenetre2.setPreferredSize(new Dimension(600, 400));

		fenetre2.setLayout(new GridLayout(2, 3));

		fenetre2.add(new JLabel("Texte1"));
		fenetre2.add(new JTextArea("Vous pouvez modifier ce texte",4,15));
		fenetre2.add(new JCheckBox("cochez moi !"));
		fenetre2.add(new JButton("clic ?"));
		fenetre2.add(new JLabel("Texte2"));
		JTextArea t = new JTextArea("Vous ne pouvez pas modifier celui-ci",4,15);
		t.setEditable(false);
		fenetre2.add(t);

		fenetre2.pack();
		fenetre2.setVisible(true);
	}

}
