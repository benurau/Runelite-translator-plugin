/*
 * Copyright (c) 2017, Aria <aria@ar1as.space>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.translator;

import com.google.inject.Provides;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Player;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.Actor;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

@PluginDescriptor(
        name = "translator",
        description = "translate npc, items and objects",
        tags = {"actions"}
)
public class TranslatorPlugin extends Plugin
{

    @Inject
    private TranslatorConfig config;
    @Inject
    private Client client;
    @Provides
    TranslatorConfig provideConfig(ConfigManager configManager) {return configManager.getConfig(TranslatorConfig.class);}

    private HashMap<String, String> itemsMap;
    private HashMap<String, String> npcMap;
    private HashMap<String, String> objectMap;
    private HashMap<String, String> dialogueMap;
    private Actor actor;


    @Override
    protected void startUp() throws Exception
    {
        updateLanguage();
    }


    public static HashMap<String, String> parseDialogue(String filepath) {
        try {
            File myObj = new File(filepath);
            HashMap<String, String> words = new HashMap<String, String>();

            Scanner myReader = new Scanner(myObj, "UTF-8");
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String[] temp = data.split(";");
                if (temp.length > 1) {
                    words.put(temp[0], temp[1]);
                }
            }
            myReader.close();
            return words;
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return null;
        }
    }

    public static HashMap<String, String> parse(String filepath) {
        try {
            File myObj = new File(filepath);
            HashMap<String, String> words = new HashMap<String, String>();

            Scanner myReader = new Scanner(myObj, "UTF-8");
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String[] temp = data.split(",");
                if (temp.length > 2) {
                    words.put(temp[0], temp[2]);
                    System.out.println(temp[0] + temp[2] + "\n");
                }
            }
            myReader.close();
            return words;
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return null;
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!event.getGroup().equals("translator"))
        {
            return;
        }
        updateLanguage();
    }

    private void updateLanguage()
    {
        switch (config.selectLanguage())
        {
            case Finnish:
                itemsMap = parse("src/main/java/com/translator/translated_languages/fi_items.txt");
                npcMap = parse("src/main/java/com/translator/translated_languages/fi_npc.txt");
                objectMap = parse("src/main/java/com/translator/translated_languages/fi_object.txt");
                dialogueMap = parseDialogue("src/main/java/com/translator/translated_languages/fi_dialogue.txt");
                break;
            /*case German:
                itemsMap = parse("src/main/java/com/translator/translated_languages/de_items.txt");
                npcMap = parse("src/main/java/com/translator/translated_languages/de_npc.txt");
                objectMap = parse("src/main/java/com/translator/translated_languages/de_object.txt");
                break;
            case Swedish:
                itemsMap = parse("src/main/java/com/translator/translated_languages/swe_items.txt");
                npcMap = parse("src/main/java/com/translator/translated_languages/swe_npc.txt");
                objectMap = parse("src/main/java/com/translator/translated_languages/swe_object.txt");
                break;*/
        }
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event)
    {
        MenuEntry[] menuEntries = client.getMenuEntries();
        MenuEntry[] newMenuEntries = Arrays.copyOf(menuEntries, menuEntries.length);

        for (int idx = 1; idx < newMenuEntries.length; idx++) {
            MenuEntry entry = newMenuEntries[idx];

            //worn items
            if (entry.getWidget() != null && WidgetInfo.TO_GROUP(entry.getWidget().getId()) == WidgetID.EQUIPMENT_GROUP_ID) {
                System.out.println("wornitem");
                translateMenuEntrys(this.itemsMap, entry, entry.getWidget().getChild(1).getItemId());
            }
            //items
            else if (entry.getItemId() > 0) {
                System.out.println(entry.getActor() + "actor");
                System.out.println(entry.getIdentifier() + "id");
                System.out.println("item");
                translateMenuEntrys(this.itemsMap, entry, entry.getItemId());
            }
            //ground items
            else if (entry.getType() == MenuAction.EXAMINE_ITEM_GROUND | entry.getType() == MenuAction.GROUND_ITEM_THIRD_OPTION ) {
                System.out.println("ground item");
                translateMenuEntrys(this.itemsMap, entry, entry.getIdentifier());
            }
            //not item
            else if (entry.getItemId() == -1) {
                //player
                if (entry.getPlayer() != null) {
                    System.out.println("player");
                }
                //npc
                else if (entry.getNpc() != null) {
                    System.out.println("npc");
                    translateMenuEntrys(this.npcMap, entry, entry.getNpc().getId());
                }
                //object
                else if (entry.getIdentifier() > 0 & entry.getType() != MenuAction.CC_OP & entry.getType() != MenuAction.RUNELITE & entry.getType() != MenuAction.WALK && entry.getType() != MenuAction.CC_OP_LOW_PRIORITY) {
                    System.out.println("object");
                    translateMenuEntrys(this.objectMap, entry, entry.getIdentifier());
                }
            }
        }
        client.setMenuEntries(newMenuEntries);
    }

    public void translateMenuEntrys(HashMap<String, String> words, MenuEntry menuEntry, Integer id){

        String target = menuEntry.getTarget();

        if (target.length() > 0) {
            String translated = words.get(id.toString());
            String[] subStrings = target.split(">");
            int colStart = subStrings[0].length() + 1;
            String colour = target.substring(0, colStart);

            if (subStrings.length > 2) {
                //npc with combat lvl
                int combatStart = target.split("<")[1].length() + 1;
                String combat = target.substring(combatStart);

                if (translated != null) {
                    menuEntry.setTarget(colour + translated + combat);
                }
            } else {
                //ground items x amount
                String[] itemSubStrings = target.split("\\(");

                if (itemSubStrings.length > 1){
                    int amountStart = target.split("\\(")[0].length() - 1;
                    String amount = target.substring(amountStart);

                    if (translated != null) {
                        menuEntry.setTarget(colour + translated + amount);
                    }
                }
                else if (translated != null) {
                    //items
                    menuEntry.setTarget(colour + translated);
                }
            }
        }
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event)
    {
        if (event.getTarget() == null || event.getSource() != client.getLocalPlayer()) {
            return;
        }
        actor = event.getTarget();

    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (actor != null)
        {
            checkWidgetDialogs();
        }
    }

    private void checkWidgetDialogs()
    {
        Widget npcTextWidget = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);
        String npcDialogText = (npcTextWidget != null) ? npcTextWidget.getText() : null;
        Widget playerTextWidget = client.getWidget(WidgetInfo.DIALOG_PLAYER_TEXT);
        String playerDialogText = (playerTextWidget != null) ? playerTextWidget.getText() : null;

        String npcdialogue = npcDialogText != null ? npcDialogText.replace("<br>", " ") : null;
        String playerdialogue = playerDialogText != null ? playerDialogText.replace("<br>", " ") : null;

        if (npcdialogue!= null && dialogueMap.get(npcdialogue) != null) {
            npcTextWidget.setText(dialogueMap.get(npcdialogue));
        }
        if (playerdialogue!= null && dialogueMap.get(playerdialogue) != null) {
            playerTextWidget.setText(dialogueMap.get(playerdialogue));
        }
    }
}
