package me.smickles.DynamicMarket;

import org.bukkit.command.CommandSender;

public class CommandHelper extends DynamicMarket{

	String message;
	CommandSender sender;

	public CommandHelper(String message, CommandSender sender) {

		super();
		this.message = message;
		this.sender = sender;
		
		this.sender.sendMessage(this.message);
	}

	public void marketAddHelp() {

		sender.sendMessage("market add requires");
		sender.sendMessage("[number] [data] [name] [value] [minvalue] [maxvalue] [changerate] [spread]");
		sender.sendMessage("as");
		sender.sendMessage("<###> <##> <abc> <#.#> <#.#> <#.#> <#.#> <#.#>");
	}

	public void marketRemoveHelp() {
		// TODO Auto-generated method stub
		
	}

}
