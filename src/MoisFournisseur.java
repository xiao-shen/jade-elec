import java.util.List;


public class MoisFournisseur {	
	public double CA;
	public double benef;
	public double prix_de_vente;
	public int nb_clients;
	public boolean has_transporteur;
	
	public MoisFournisseur(double CA, double benef, double prix_de_vente, int nb_clients, boolean has_transporteur)
	{
		this.CA = MainLauncher.roundCurrency(CA);
		this.benef = MainLauncher.roundCurrency(benef);
		this.prix_de_vente = MainLauncher.roundCurrency(prix_de_vente);
		this.nb_clients = nb_clients;
		this.has_transporteur = has_transporteur;
	}
	
	public String getUserMessage()
	{
		return "encaissé " + CA + "\n"
				+ "bénéfice de " + benef + "\n"
				+ "vente à " + prix_de_vente + "\n"
				+ nb_clients + " clients" + "\n"
				+ "transporteur " + (has_transporteur ? "interne" : "ERDF");
	}
	
	public String getCSVLine()
	{
		return CA + MainLauncher.SEP + benef + MainLauncher.SEP + 
				prix_de_vente + MainLauncher.SEP + nb_clients + MainLauncher.SEP 
				+ (has_transporteur ? "1" : "0") + MainLauncher.SEP;
	}
	
	public static String generateCSV(List<MoisFournisseur> hist)
	{
		String out = "";
		int mois = 0;
		for (MoisFournisseur elt : hist)
		{
			out += mois + MainLauncher.SEP + elt.getCSVLine() + "\n";
			mois++;
		}
		return out;
	}
}
