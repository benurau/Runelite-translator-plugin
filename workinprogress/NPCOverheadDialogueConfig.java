package com.translator;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("NPCOverheadCDialogue")
public interface NPCOverheadDialogueConfig extends Config
{
    @ConfigItem(
            keyName = "dialogBoxText",
            name = "Display Dialog Box Text Overhead",
            description = "Displays dialog in the dialog box above the corresponding NPC"
    )
    default boolean showDialogBoxText()
    {
        return false;
    }


    @ConfigItem(
            keyName = "chatDialog",
            name = "Display Overhead Dialog in Chat",
            description = "Displays all enabled dialog in the chat"
    )
    default boolean enableChatDialog() { return false; }
}