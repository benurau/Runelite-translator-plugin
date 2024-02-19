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
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.ItemComposition;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;



@PluginDescriptor(
        name = "translator",
        description = "translate npc, items, objects and dialogue",
        tags = {"actions"}
)
public class TranslatorPlugin extends Plugin
{

    @Inject
    private TranslatorConfig config;
    @Inject
    private Client client;
    @Inject
    private ItemManager itemManager;
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


    public HashMap<String, String> parseDialogue(String filepath) {
        HashMap<String, String> words = new HashMap<String, String>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(filepath)))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] temp = line.split(";");
                if (temp.length > 1) {
                    words.put(temp[0], temp[1]);
                }
            }
            return words;
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return null;
        }
    }

    public HashMap<String, String> parse(String filepath) {
        HashMap<String, String> words = new HashMap<String, String>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(filepath)))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] temp = line.split(",");
                if (temp.length > 2) {
                    words.put(temp[0], temp[2]);
                }
            }
            return words;
        } catch (IOException e) {
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
        itemsMap = parse("/"+ config.selectLanguage() +"_items.txt");
        npcMap = parse("/"+ config.selectLanguage() +"_npc.txt");
        objectMap = parse("/"+ config.selectLanguage() +"_object.txt");
        dialogueMap = parseDialogue("/"+ config.selectLanguage() +"_dialogue.txt");
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event)
    {
        MenuEntry[] menuEntries = client.getMenuEntries();
        MenuEntry[] newMenuEntries = Arrays.copyOf(menuEntries, menuEntries.length);

        for (int idx = 1; idx < newMenuEntries.length; idx++) {
            MenuEntry entry = newMenuEntries[idx];

            //item
            if (entry.getItemId() > 0) {
                translateMenuEntrys(this.itemsMap, entry, entry.getItemId());
            }
            //equipped item
            if (entry.getWidget() != null && entry.getWidget().getId() <= 25362457 && entry.getWidget().getId() >= 25362447) {
                translateMenuEntrys(this.itemsMap, entry, entry.getWidget().getChild(1).getItemId());
            }
            //ground items
            else if (entry.getType() == MenuAction.EXAMINE_ITEM_GROUND | entry.getType() == MenuAction.GROUND_ITEM_THIRD_OPTION ) {
                translateMenuEntrys(this.itemsMap, entry, entry.getIdentifier());
            }
            //not item
            else if (entry.getItemId() == -1) {
                //player
                if (entry.getPlayer() != null) {
                }
                //npc
                else if (entry.getNpc() != null) {
                    translateMenuEntrys(this.npcMap, entry, entry.getNpc().getId());
                }
                //object
                else if (entry.getIdentifier() > 0 & entry.getType() != MenuAction.CC_OP & entry.getType() != MenuAction.RUNELITE & entry.getType() != MenuAction.WALK && entry.getType() != MenuAction.CC_OP_LOW_PRIORITY) {
                    translateMenuEntrys(this.objectMap, entry, entry.getIdentifier());
                }
            }
        }
        client.setMenuEntries(newMenuEntries);
    }

    public String notedItemsCheck(HashMap<String, String> words, Integer id){
        String translated = null;
        if (words == itemsMap) {
            final ItemComposition itemComp = itemManager.getItemComposition(id);
            if (itemComp.getNote() == 799) {
                translated = words.get(String.valueOf(itemComp.getLinkedNoteId()));
            }
        }
        return translated;
    }

    public void translateMenuEntrys(HashMap<String, String> words, MenuEntry menuEntry, Integer id){
        String target = menuEntry.getTarget();

        if (target.length() > 0) {
            String translated = words.get(id.toString());
            String notedId = notedItemsCheck(words, id);
            if (notedId != null){
                translated = notedId;
            }
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
            checkWidgetOptionDialogs();
        }
    }

    private void checkWidgetOptionDialogs()
    {
        Widget playerOptionsWidget = client.getWidget(ComponentID.DIALOG_OPTION_OPTIONS);
        //String playerOptionsText = (playerTextWidget != null) ? playerTextWidget.getText() : null;
        Widget[] optionWidgets = playerOptionsWidget.getChildren();
        for (Widget i: optionWidgets){
            String optionText = i.getText() != null ? i.getText().replace("<br>", " ") : null;
            System.out.println(dialogueMap.get(optionText));
            if (optionText != null && dialogueMap.get(optionText) != null){
                i.setText(dialogueMap.get(optionText));
            }
        }
    }

    private void checkWidgetDialogs()
    {
        Widget npcTextWidget = client.getWidget(ComponentID.DIALOG_NPC_TEXT);
        String npcDialogText = (npcTextWidget != null) ? npcTextWidget.getText() : null;
        Widget playerTextWidget = client.getWidget(ComponentID.DIALOG_PLAYER_TEXT);
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
