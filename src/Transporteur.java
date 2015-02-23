
public class Transporteur {
	private double cout_watt;
	public Transporteur(double cout_watt)
	{
		this.cout_watt = cout_watt;
	}
	public double getPrice(double conso_totale)
	{
		return cout_watt * conso_totale;
	}
}
