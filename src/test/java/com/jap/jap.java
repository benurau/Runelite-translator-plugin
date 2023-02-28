package com.jap;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class jap
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(HighlightPlugin.class);
		RuneLite.main(args);
	}
}