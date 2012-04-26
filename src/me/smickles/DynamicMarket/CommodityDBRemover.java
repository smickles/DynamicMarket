package me.smickles.DynamicMarket;

public class CommodityDBRemover {
	
	DynamicMarket plugin;

	public CommodityDBRemover(DynamicMarket plugin) {

		this.plugin = plugin;
	}

	public void removeCommodityByName(String name) throws CommodityNotFoundException {
		
    	Commodity c = plugin.getDatabase().find(Commodity.class)
    			.where()
    			.ieq("name", name)
    			.findUnique();
    	
    	if (name == null)
    		throw new CommodityNotFoundException("No commodity \"" + name + "\" found in the database");
    	
    	plugin.getDatabase().delete(c);
	}

}
