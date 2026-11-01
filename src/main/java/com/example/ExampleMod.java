package com.example;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Mod(modid = "catclient", name = "Cat Client", version = "1.0", acceptedMinecraftVersions = "")
public class CatClient {

    // Estados dos Módulos do Cliente
    private boolean toggleSprint = true;
    private boolean perspective360 = false;

    // Listas leves para contar os cliques por segundo (CPS)
    private final List<Long> leftClicks = new ArrayList<>();
    private final List<Long> rightClicks = new ArrayList<>();

    // Sistema de Economia Nativo (CatCoins)
    private int catCoins = 0;
    private File saveFile;

    // Teclas de Ativação (Controles)
    private static KeyBinding keySprint;
    private static KeyBinding keyPerspective;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        
        // Registra os botões na aba de Opções -> Controles do Minecraft
        keySprint = new KeyBinding("Ativar/Desativar Sprint", Keyboard.KEY_I, "Cat Client");
        keyPerspective = new KeyBinding("Perspective 360", Keyboard.KEY_V, "Cat Client");

        // Cria a pasta e o arquivo para salvar as moedas no celular/PojavLauncher
        File configDir = new File(Minecraft.getMinecraft().mcDataDir, "config");
        if (!configDir.exists()) configDir.mkdirs();
        saveFile = new File(configDir, "catcoins.txt");
        loadCoins();
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && toggleSprint) {
            // Força a corrida automática apenas se estiver andando para frente e não agachado
            if (mc.thePlayer.moveForward > 0 && !mc.thePlayer.isSneaking()) {
                mc.thePlayer.setSprinting(true);
            }
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (keySprint.isPressed()) {
            toggleSprint = !toggleSprint;
        }
        if (keyPerspective.isPressed()) {
            perspective360 = !perspective360;
            Minecraft mc = Minecraft.getMinecraft();
            if (perspective360 && mc.thePlayer != null) {
                mc.gameSettings.thirdPersonView = 3; // Muda para a visão livre traseira
            } else {
                mc.gameSettings.thirdPersonView = 0; // Retorna para primeira pessoa
            }
        }
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        // Verifica se o ataque derrotou totalmente o alvo para pagar o jogador
        if (event.target instanceof EntityLivingBase) {
            EntityLivingBase target = (EntityLivingBase) event.target;
            if (target.getHealth() <= 0 || target.isDead) {
                catCoins++;
                saveCoins();
                if (Minecraft.getMinecraft().thePlayer != null) {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText("§a+1 CatCoin obtida!")
                    );
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderHUD(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getMinecraft();
            // Só exibe a interface se o menu estiver fechado e o jogador estiver ativo no mundo
            if (mc.currentScreen == null && mc.thePlayer != null) {
                
                long currentTime = System.currentTimeMillis();
                
                // Registra o tempo exato dos cliques atuais do mouse
                if (Mouse.isButtonDown(0)) {
                    if (leftClicks.isEmpty() || currentTime - leftClicks.get(leftClicks.size() - 1) > 50) {
                        leftClicks.add(currentTime);
                    }
                }
                if (Mouse.isButtonDown(1)) {
                    if (rightClicks.isEmpty() || currentTime - rightClicks.get(rightClicks.size() - 1) > 50) {
                        rightClicks.add(currentTime);
                    }
                }

                // Remove cliques registrados há mais de 1 segundo para atualizar o CPS em tempo real
                leftClicks.removeIf(click -> click < currentTime - 1000);
                rightClicks.removeIf(click -> click < currentTime - 1000);

                // Estilo visual clássico (Fundo escuro sem comprometer FPS no celular)
                int fundoMod = 0x90000000; 
                int textoBranco = 0xFFFFFF;   
                int textoVerde = 0x00FF00;   

                // 1. Renderização do ToggleSprint
                Gui.drawRect(5, 5, 110, 20, fundoMod);
                mc.fontRendererObj.drawString("Sprint: " + (toggleSprint ? "ON" : "OFF"), 10, 8, toggleSprint ? textoVerde : 0xFF0000);

                // 2. Renderização do Keystrokes (W, A, S, D)
                boolean w = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
                boolean a = Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());
                boolean s = Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
                boolean d = Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());

                Gui.drawRect(30, 25, 50, 45, w ? 0x90FFFFFF : fundoMod);
                mc.fontRendererObj.drawString("W", 37, 30, w ? 0x000000 : textoBranco);

                Gui.drawRect(8, 48, 28, 68, a ? 0x90FFFFFF : fundoMod);
                mc.fontRendererObj.drawString("A", 15, 53, a ? 0x000000 : textoBranco);

                Gui.drawRect(30, 48, 50, 68, s ? 0x90FFFFFF : fundoMod);
                mc.fontRendererObj.drawString("S", 37, 53, s ? 0x000000 : textoBranco);

                Gui.drawRect(52, 48, 72, 68, d ? 0x90FFFFFF : fundoMod);
                mc.fontRendererObj.drawString("D", 59, 53, d ? 0x000000 : textoBranco);

                // 3. Renderização do CPS
                String textoCps = "L: " + leftClicks.size() + " | R: " + rightClicks.size() + " CPS";
                Gui.drawRect(5, 73, 110, 88, fundoMod);
                mc.fontRendererObj.drawString(textoCps, 10, 76, textoBranco);

                // 4. Renderização do Painel de CatCoins
                String textoMoedas = "🐱 CatCoins: " + catCoins;
                Gui.drawRect(5, 93, 110, 108, fundoMod);
                mc.fontRendererObj.drawString(textoMoedas, 10, 96, 0xFFFF00);
            }
        }
    }

    // Grava e lê os dados no dispositivo para salvar o progresso das moedas
    private void saveCoins() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(saveFile))) {
            writer.println(catCoins);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadCoins() {
        if (!saveFile.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(saveFile))) {
            String linha = reader.readLine();
            if (linha != null) {
                catCoins = Integer.parseInt(linha.trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
