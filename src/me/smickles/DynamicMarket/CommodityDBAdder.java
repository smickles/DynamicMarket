package me.smickles.DynamicMarket;

public class CommodityDBAdder extends DynamicMarket{

	public CommodityDBAdder() {
		// TODO Auto-generated constructor stub
	}

	public void addCommodity(Commodities commodity) throws DuplicateCommodityException {
		
    	Commodities check = getDatabase().find(Commodities.class)
    			.where()
    			.ieq("number", String.valueOf(commodity.getNumber()))
    			.ieq("data", String.valueOf(commodity.getData()))
    			.findUnique();
    	
    	if (check != null)
    		throw new DuplicateCommodityException("Commodity is already in the database");
    	
    	getDatabase().save(commodity);
		
	}

}
