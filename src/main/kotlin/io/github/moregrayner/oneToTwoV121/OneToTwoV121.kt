package io.github.moregrayner.oneToTwoV121

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class OneToTwoV121 : JavaPlugin(), Listener {
    private val playerGroups = mutableMapOf<Player, Player>()
    private val groupDeathCount = mutableMapOf<Player, Int>()
    private val lastUsedTime = mutableMapOf<Player, Long>()

    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this)
        this.getCommand("teammatesetter")?.setExecutor { sender, _, _, _ ->
            if (sender is Player) {
                if (sender.isOp) {
                    randomGroups()
                    sender.sendMessage("팀원이 정상적으로 배정되었습니다.")
                    actionBarUpdate()
                } else {
                    sender.sendMessage(ChatColor.RED.toString() + "명령어 사용이 금지됩니다.")
                }
            }
            true
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        player.inventory.addItem(ItemStack(Material.COMPASS))
    }

    private fun randomGroups() {
        val players = Bukkit.getOnlinePlayers().toMutableList()
        if (players.size % 2 != 0) {
            players.add(null)
        }

        for (i in players.indices step 2) {
            val player1 = players[i]
            val player2 = players.getOrNull(i + 1)

            if (player1 != null && player2 != null) {
                playerGroups[player1] = player2
                playerGroups[player2] = player1
            }
        }

        for (player in playerGroups.keys) {
            groupDeathCount[player] = 0
        }
    }

    private fun actionBarUpdate() {
        object : BukkitRunnable() {
            override fun run() {
                for (player in playerGroups.keys) {
                    if (player.isOnline) {
                        val groupMate = playerGroups[player]
                        val message = "지정된 그룹원: ${groupMate?.name ?: player.name}"
                        val actionBarMessage = TextComponent(ChatColor.GOLD.toString() + message)
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, actionBarMessage)
                    }
                }
            }
        }.runTaskTimer(this, 0L, 40L)
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        if (event.entity is Player) {
            val player = event.entity as Player

            if (playerGroups.containsKey(player)) {
                val groupMate = playerGroups[player]
                val damage = event.damage

                groupMate?.let {
                    if (it.isOnline) {
                        it.damage(damage)
                        event.isCancelled = true
                    }
                }
            }
        }
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        if (event.entity is Player) {
            val player = event.entity as Player

            if (playerGroups.containsKey(player)) {
                val groupMate = playerGroups[player]

                groupDeathCount[player] = groupDeathCount[player]?.plus(1) ?: 1
                groupMate?.let {
                    groupDeathCount[it] = groupDeathCount[it]?.plus(1) ?: 1
                    it.health = 0.0
                }
            }
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.item?.type == Material.COMPASS) {
            val player = event.player
            val currentTime = System.currentTimeMillis()
            val cooldownTime = 10000L

            val lastTime = lastUsedTime[player]
            if (lastTime != null && currentTime - lastTime < cooldownTime) {
                val remainingTime = cooldownTime - (currentTime - lastTime)
                player.sendMessage(ChatColor.RED.toString() + "쿨다운 중입니다... ${remainingTime / 1000}초.")
                return
            }

            lastUsedTime[player] = currentTime
            if (playerGroups.containsKey(player)) {
                val groupMate = playerGroups[player]
                groupMate?.let {
                    if (it.isOnline) {
                        player.sendMessage("${player.name} 님의 그룹원 ${it.name} 님의 좌표: " +
                                "${it.location.blockX}, " +
                                "${it.location.blockY}, " +
                                "${it.location.blockZ}")
                    }
                }
            }
        }
    }
}
