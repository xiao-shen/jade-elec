import java.util.List;

public class MoisConsommateur {
	public int fournisseurChoisi;
	public double consommation;
	public double production;
	
	public MoisConsommateur(int fournisseurChoisi, double consommation, double production) {
		this.fournisseurChoisi = fournisseurChoisi;
		this.consommation = MainLauncher.roundCurrency(consommation);
		this.production = MainLauncher.roundCurrency(production);
	}
	public String getUserMessage()
	{
		return "consommé " + consommation 
				+ "\nproduit " + production 
				+ "\nau fournisseur " + fournisseurChoisi;
	}
	
	public String getCSVLine()
	{
		return consommation + MainLauncher.SEP + production + MainLauncher.SEP + 
				fournisseurChoisi + MainLauncher.SEP;
	}
	
	public static String generateCSV(List<MoisConsommateur> hist)
	{
		String out = "";
		int mois = 0;
		for (MoisConsommateur elt : hist)
		{
			out += mois + MainLauncher.SEP + elt.getCSVLine() + "\n";
			mois++;
		}
		return out;
	}
}