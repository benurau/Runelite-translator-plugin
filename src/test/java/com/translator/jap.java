package com.translator;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class jap
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TranslatorPlugin.class);
		RuneLite.main(args);
	}
}