package me.smickles.DynamicMarket;

public class CommodityDBAdder extends DynamicMarket{
	
	public DynamicMarket plugin; 

	public CommodityDBAdder(DynamicMarket plugin) {
		
		this.plugin = plugin;
	}

	public void addCommodity(Commodities commodity) throws DuplicateCommodityException {
		
		Commodities check = plugin.getDatabase().find(Commodities.class)
    			.where()
    			.ieq("number", String.valueOf(commodity.getNumber()))
    			.ieq("data", String.valueOf(commodity.getData()))
    			.findUnique();
    	
    	if (check != null)
    		throw new DuplicateCommodityException("Commodity is already in the database");
    	
    	plugin.getDatabase().save(commodity);
		
	}

}
