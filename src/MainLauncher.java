import java.sql.ClientInfoStatus;
import java.util.Random;

import jade.core.AID;
import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.wrapper.*;

public class MainLauncher {
	public static final String COMMON_LANGUAGE = "IEC"; // International Electrotechnical Commission
	public static final String COMMON_ONTOLOGY = "CIM"; // Common Information Model
	public static AID monitor;
	public static Transporteur erdf = new Transporteur(0.20);
	public static final double prix_rachat = 0.6;
	public static final double prix_transporteur = 20000;

	private static Random rd = new Random();
	public static double gaussianRandom(double mean, double sigma)
	{
		return mean + sigma*rd.nextGaussian();
	}
	
	// entier aléatoire supérieur ou égal à min, strictement inférieur à max
	public static int intRandom(int min, int max)
	{
		return min+rd.nextInt(max-min);
	}
	
	public static void main(String[] args) {
		// Création du runtime
		Runtime rt = Runtime.instance();
		rt.setCloseVM(true);
		// Lancement de la plateforme
		Profile pMain = new ProfileImpl("localhost", 8888, null);
		AgentContainer mc = rt.createMainContainer(pMain);
		// lancement des agents
		try {
			AgentController F1, F2, H, C1, C2, C3, C4, M;
			F1 = 
					mc.createNewAgent("F1", FournisseurAgent.class.getName(), new Object[0]);
			F2 = 
					mc.createNewAgent("F2", FournisseurAgent.class.getName(), new Object[0]);
			H = 
					mc.createNewAgent("HRL", HorlogeAgent.class.getName(), new Object[0]);
			C1 = 
					mc.createNewAgent("C1", ConsommateurAgent.class.getName(), new Object[0]);
			C2 = 
					mc.createNewAgent("C2", ConsommateurAgent.class.getName(), new Object[0]);
			C3 = 
					mc.createNewAgent("C3", ConsommateurAgent.class.getName(), new Object[0]);
			C4 = 
					mc.createNewAgent("C4", ConsommateurAgent.class.getName(), new Object[0]);
			M =
					mc.createNewAgent("MON", Observateur.class.getName(), new Object[0]);
			M.start();
			H.start();
			F1.start();
			F2.start();
			C1.start();
			C2.start();
			C3.start();
			C4.start();
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}

}
