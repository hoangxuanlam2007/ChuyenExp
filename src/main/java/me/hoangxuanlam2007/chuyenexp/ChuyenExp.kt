package me.hoangxuanlam2007.chuyenexp

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.command.TabCompleter
import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.util.*
import java.io.BufferedWriter
import java.nio.charset.StandardCharsets

data class ConfirmationData(val sender: Player, val target: Player, val amount: Int, val timeout: Long)

class ChuyenExp : JavaPlugin(), CommandExecutor, TabCompleter {

    private val cooldowns: MutableMap<UUID, Long> = mutableMapOf()
    private val confirmations: MutableMap<UUID, ConfirmationData> = mutableMapOf()
    private lateinit var customMessages: FileConfiguration
    private lateinit var config: FileConfiguration
    private var cooldownDuration: Long = 5000
    private var maxTransferAmount: Int = 10000000
    private var confirmationTimeout: Long = 15000 // Default confirmation timeout in seconds

    override fun onEnable() {
        // Initialize command
        getCommand("cexp")?.setExecutor(this)
        getCommand("cexp")?.tabCompleter = this
        logger.info("[Chuyen Exp] Plugin is enabled!")

        // Load or create custom_messages.yml
        val dataFolder = dataFolder // Assuming dataFolder is the plugin's data folder
        if (!dataFolder.exists()) {
            dataFolder.mkdirs() // Create the necessary directories
        }

        val customMessagesFile = File(dataFolder, "custom_messages.yml")
        if (!customMessagesFile.exists()) {
            try {
                customMessagesFile.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            // Add comment for easier configuration
            try {
                val writer = BufferedWriter(OutputStreamWriter(FileOutputStream(customMessagesFile), StandardCharsets.UTF_8))
                writer.write("# ===================================================================================================================|")
                writer.newLine()
                writer.write("# > This Plugin is coded privately by ChimmFX aka Hoàng Xuân Lâm. <")
                writer.newLine()
                writer.write("# - Every feature included cooldown, maxTransferAmount, etc. was suggested by another admin that I'm working with.")
                writer.newLine()
                writer.write("# - Feel safe to use this plugin!")
                writer.newLine()
                writer.write("#")
                writer.newLine()
                writer.write("# - There are 5 permissions that work perfectly with any permissions plugin, such as LuckPerm:")
                writer.newLine()
                writer.write("#     + chuyenexp")
                writer.newLine()
                writer.write("#     + chuyenexp.use")
                writer.newLine()
                writer.write("#     + chuyenexp.bypass.cooldown")
                writer.newLine()
                writer.write("#     + chuyenexp.bypass.maxtransfer")
                writer.newLine()
                writer.write("#     + chuyenexp.reload")
                writer.newLine()
                writer.write("# - These permissions work as their names suggest.")
                writer.newLine()
                writer.write("#")
                writer.newLine()
                writer.write("# For further information, to report bugs, or to contact me, email: chim31102007@gmail.com")
                writer.newLine()
                writer.write("# Author Github: https://github.com/hoangxuanlam2007")
                writer.newLine()
                writer.write("# ===================================================================================================================|")
                writer.newLine()
                writer.newLine()
                writer.newLine()
                writer.newLine()
                writer.flush()
                writer.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            customMessages = YamlConfiguration.loadConfiguration(customMessagesFile)

            // Set default messages
            setDefaultMessage("command_player_only", "&6[&aChuyen Exp&6] &cThe command can only be executed by a player!")
            setDefaultMessage("command_cooldown", "&6[&aChuyen Exp&6] &cPlease wait &f{seconds} seconds &cbefore using the command again.")
            setDefaultMessage("command_usage", "&6[&aChuyen Exp&6] &fUse &e/cexp help &fto show help.")
            setDefaultMessage("command_help", "&a&k|||&r&a ===== &6[&aChuyen Exp - &eHelp &6] &a===== &k|||&r\n&e/cexp <player> <amount> &f- Transfer experience to another player.\n&e/cexp help &f- Show command help.")
            setDefaultMessage("command_amount_required", "&6[&aChuyen Exp&6] &cPlease type a specific number of &aexperience &cto transfer.")
            setDefaultMessage("command_player_not_found", "&6[&aChuyen Exp&6] &cPlayer '&b{player}' &cnot found!")
            setDefaultMessage("command_cannot_transfer_to_self", "&6[&aChuyen Exp&6] &cYou cannot transfer experience to yourself!")
            setDefaultMessage("command_invalid_amount", "&6[&aChuyen Exp&6] &cInvalid amount of experience to transfer.")
            setDefaultMessage("command_insufficient_exp", "&6[&aChuyen Exp&6] &cYou do not have enough &aexperience!")
            setDefaultMessage("command_max_transfer_amount", "&6[&aChuyen Exp&6] &cThe &5maximum transfer amount &cis &e{amount}.")
            setDefaultMessage("command_exp_transferred_sender", "&6[&aChuyen Exp&6] &aYou have transferred &l{amount} &r&aexperience to &b{target}.")
            setDefaultMessage("command_exp_transferred_target", "&6[&aChuyen Exp&6] &aYou have received &l{amount} &r&aexperience from &b{sender}.")
            setDefaultMessage("command_confirm_transfer" ,"&6[&aChuyen Exp&6] &fYou are transferring &e{amount} &fexp to &b{target}&f.\n&6 [+] &fConfirm by typing &a/cexp confirm&f\n&6 [+] &fDeny by typing &c/cexp deny\n&fThe transfer will be automatically cancelled in &e{timeout} &fseconds if not confirmed.")
            setDefaultMessage("command_transfer_cancelled" ,"&6[&aChuyen Exp&6] &cTransfer cancelled.")
            setDefaultMessage("command_transfer_cancelled_timeout" ,"&6[&aChuyen Exp&6] &cTransfer cancelled due to timeout.")
            try {
                customMessages.save(customMessagesFile)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            customMessages = YamlConfiguration.loadConfiguration(customMessagesFile)
        }

        // Load or create config.yml
        val configFile = File(dataFolder, "config.yml")
        if (!configFile.exists()) {
            try {
                configFile.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            // Write comments to the file
            try {
                val writer = BufferedWriter(OutputStreamWriter(FileOutputStream(configFile), StandardCharsets.UTF_8))
                writer.write("# ===================================================================================================================|")
                writer.newLine()
                writer.write("# > This Plugin is coded privately by ChimmFX aka Hoàng Xuân Lâm. <")
                writer.newLine()
                writer.write("# - Every feature included cooldown, maxTransferAmount, etc. was suggested by another admin that I'm working with.")
                writer.newLine()
                writer.write("# - Feel safe to use this plugin!")
                writer.newLine()
                writer.write("#")
                writer.newLine()
                writer.write("# - There are 5 permissions that work perfectly with any permissions plugin, such as LuckPerm:")
                writer.newLine()
                writer.write("#     + chuyenexp")
                writer.newLine()
                writer.write("#     + chuyenexp.use")
                writer.newLine()
                writer.write("#     + chuyenexp.bypass.cooldown")
                writer.newLine()
                writer.write("#     + chuyenexp.bypass.maxtransfer")
                writer.newLine()
                writer.write("#     + chuyenexp.reload")
                writer.newLine()
                writer.write("# - These permissions work as their names suggest.")
                writer.newLine()
                writer.write("#")
                writer.newLine()
                writer.write("# For further information, to report bugs, or to contact me, email: chim31102007@gmail.com")
                writer.newLine()
                writer.write("# Author Github: https://github.com/hoangxuanlam2007")
                writer.newLine()
                writer.write("# ===================================================================================================================|")
                writer.newLine()
                writer.newLine()
                writer.newLine()
                writer.newLine()
                writer.flush()
                writer.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            config = YamlConfiguration.loadConfiguration(configFile)

            // Set default values
            config.set("cooldown_duration", cooldownDuration)
            config.set("max_transfer_amount", maxTransferAmount)
            config.set("confirmation_timeout", confirmationTimeout) // Add the confirmation timeout value to the config

            try {
                config.save(configFile)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            config = YamlConfiguration.loadConfiguration(configFile)
            cooldownDuration = config.getLong("cooldown_duration", cooldownDuration)
            maxTransferAmount = config.getInt("max_transfer_amount", maxTransferAmount)
            confirmationTimeout = config.getLong("confirmation_timeout", confirmationTimeout) // Get the confirmation timeout value from the config
        }
    }

    override fun onDisable() {
        logger.info("[Chuyen Exp] Plugin is disabled!")
    }

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        if (cmd.name.equals("cexp", ignoreCase = true)) {
            if (sender !is Player) {
                sender.sendMessage(getMessage("command_player_only"))
                return true
            }

            if (args.size == 1 && args[0].equals("reload", ignoreCase = true)) {
                if (sender.hasPermission("chuyenexp.reload")) {
                    reloadPlugin()
                    sender.sendMessage(ChatColor.GREEN.toString() + "[Chuyen Exp] Plugin reloaded successfully.")
                } else {
                    sender.sendMessage(ChatColor.DARK_RED.toString() + "[Chuyen Exp] You don't have permission to use this command.")
                }
                return true
            }

            if (args.size == 1 && args[0].equals("confirm", ignoreCase = true)) {
                val confirmationData = confirmations[sender.uniqueId]
                if (confirmationData != null) {
                    confirmationData.completeTransfer() // Complete the transfer
                    confirmations.remove(sender.uniqueId)
                } else {
                    sender.sendMessage(getMessage("command_transfer_cancelled_timeout")) // Confirmation timeout occurred
                }
                return true
            }

            if (args.size == 1 && args[0].equals("deny", ignoreCase = true)) {
                val confirmationData = confirmations[sender.uniqueId]
                if (confirmationData != null) {
                    confirmationData.cancelTransfer()
                    confirmations.remove(sender.uniqueId)
                } else {
                    sender.sendMessage(getMessage("command_transfer_cancelled_timeout")) // Confirmation timeout occurred
                }
                return true
            }

            val senderUuid = sender.uniqueId
            val currentTime = System.currentTimeMillis()

            // Bypass Cooldown permission
            val bypassCooldown = sender.hasPermission("chuyenexp.bypass.cooldown")
            if (!bypassCooldown && cooldowns.containsKey(senderUuid)) {
                // Cooldown logic for non-bypass players
                val lastUseTime = cooldowns[senderUuid] ?: 0L
                val timeRemaining = lastUseTime + cooldownDuration - currentTime

                if (timeRemaining > 0) {
                    val secondsRemaining = Math.ceil(timeRemaining / 1000.0).toInt()
                    sender.sendMessage(getMessage("command_cooldown").replace("{seconds}", secondsRemaining.toString()))
                    return true
                }
            }
            // Bypass maxTransfer permission
            if (sender.hasPermission("chuyenexp.bypass.maxtransfer")) {
                maxTransferAmount = Int.MAX_VALUE
            }

            if (args.isEmpty()) {
                sender.sendMessage(getMessage("command_usage"))
                return true
            }

            if (args[0].equals("help", ignoreCase = true)) {
                // Handle /cexp help command
                sender.sendMessage(getMessage("command_help"))
                return true
            }

            if (args.size == 1 && args[0].equals("confirm", ignoreCase = true)) {
                val confirmationData = confirmations[sender.uniqueId]
                if (confirmationData != null) {
                    if (confirmationData.getAmount() > maxTransferAmount) {
                        sender.sendMessage(getMessage("command_max_transfer_amount").replace("{amount}", formatNumber(maxTransferAmount)))
                        return true
                    }
                    confirmationData.completeTransfer() // Complete the transfer
                    confirmations.remove(sender.uniqueId)
                } else {
                    sender.sendMessage(getMessage("command_transfer_cancelled_timeout")) // Confirmation timeout occurred
                }
                return true
            }

            if (args.size == 1 && args[0].equals("deny", ignoreCase = true)) {
                val confirmationData = confirmations[sender.uniqueId]
                if (confirmationData != null) {
                    confirmationData.cancelTransfer()
                    confirmations.remove(sender.uniqueId)
                } else {
                    sender.sendMessage(getMessage("command_transfer_cancelled_timeout")) // Confirmation timeout occurred
                }
                return true
            }

            if (args.size == 1) {
                sender.sendMessage(getMessage("command_amount_required"))
                return true
            }

            val targetPlayer: Player? = Bukkit.getPlayerExact(args[0])
            if (targetPlayer == null) {
                sender.sendMessage(getMessage("command_player_not_found").replace("{player}", args[0]))
                return true
            }

            if (sender == targetPlayer) {
                sender.sendMessage(getMessage("command_cannot_transfer_to_self"))
                return true
            }

            val amount: Int = try {
                args[1].toInt()
            } catch (e: NumberFormatException) {
                sender.sendMessage(getMessage("command_invalid_amount"))
                return true
            }

            if (amount <= 0) {
                sender.sendMessage(getMessage("command_invalid_amount"))
                return true
            }

            val senderExp: Int = sender.totalExperience

            if (senderExp < amount) {
                sender.sendMessage(getMessage("command_insufficient_exp"))
                return true
            }

            if (amount > maxTransferAmount) {
                sender.sendMessage(getMessage("command_max_transfer_amount").replace("{amount}", formatNumber(maxTransferAmount)))
            } else {
                // Prompt confirmation
                val confirmationData = ConfirmationData(sender, targetPlayer, amount, currentTime)
                confirmationData.startConfirmation()
                return true
            }
        }

        return false
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<String>): List<String>? {
        if (cmd.name.equals("cexp", ignoreCase = true)) {
            if (args.size == 1) {
                val suggestions = mutableListOf<String>()
                suggestions.add("help") // Add 'help' as a suggestion

                // Add suggestion of player names excluding the sender
                val onlinePlayers = Bukkit.getOnlinePlayers().map { it.name }.filter { it != sender.name }
                suggestions.addAll(onlinePlayers)

                if (sender.hasPermission("chuyenexp.reload")) {
                    suggestions.add("reload") // Add 'reload' as a suggestion for users with OP or permission
                }

                return suggestions
            }
            if (args.size == 2) {
                return emptyList()
            }
        }
        return null
    }

    private fun setDefaultMessage(key: String, defaultValue: String) {
        if (!customMessages.contains(key)) {
            customMessages.set(key, ChatColor.translateAlternateColorCodes('&', defaultValue))
            try {
                customMessages.save(File(dataFolder, "custom_messages.yml"))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun getMessage(key: String): String {
        val message = customMessages.getString(key, "") ?: ""
        return ChatColor.translateAlternateColorCodes('&', message)
    }

    private fun formatNumber(number: Int): String {
        return NumberFormat.getInstance().format(number)
    }

    private fun reloadPlugin() {
        // Disable the plugin
        Bukkit.getPluginManager().disablePlugin(this)

        // Enable the plugin
        Bukkit.getPluginManager().enablePlugin(this)
    }

    private inner class ConfirmationData(
        private val sender: Player,
        private val targetPlayer: Player,
        private val amount: Int,
        private val confirmationStartTime: Long
    ) {
        private var confirmationTaskId: Int = -1

        fun getSender(): Player {
            return sender
        }

        fun getAmount(): Int {
            return amount
        }

        fun getTargetPlayer(): Player {
            return targetPlayer
        }

        fun startConfirmation() {
            val confirmationMessage = getMessage("command_confirm_transfer")
                .replace("{sender}", sender.name)
                .replace("{amount}", formatNumber(amount))
                .replace("{target}", targetPlayer.name)
                .replace("{timeout}", (confirmationTimeout / 1000).toString())

            sender.sendMessage(confirmationMessage)

            confirmationTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this@ChuyenExp, {
                if (confirmations.containsKey(sender.uniqueId)) {
                    confirmations.remove(sender.uniqueId)
                    sender.sendMessage(getMessage("command_transfer_cancelled_timeout"))
                }
            }, confirmationTimeout / 50) // Delay in ticks (50 ticks = 1 second)

            confirmations[sender.uniqueId] = this
        }

        fun completeTransfer() {
            val senderExp: Int = sender.totalExperience

            if (senderExp < amount) {
                sender.sendMessage(getMessage("command_insufficient_exp"))
                return
            }

            if (amount > maxTransferAmount) {
                sender.sendMessage(getMessage("command_max_transfer_amount").replace("{amount}", formatNumber(maxTransferAmount)))
                return
            }

            sender.giveExp(-amount)
            sender.sendMessage(getMessage("command_exp_transferred_sender")
                .replace("{amount}", formatNumber(amount))
                .replace("{target}", targetPlayer.name))
            logger.info("${sender.name} just transferred ${formatNumber(amount)} exp to ${targetPlayer.name}")

            targetPlayer.giveExp(amount)
            targetPlayer.sendMessage(getMessage("command_exp_transferred_target")
                .replace("{amount}", formatNumber(amount))
                .replace("{sender}", sender.name))

            // Set cooldown for the sender
            cooldowns[sender.uniqueId] = System.currentTimeMillis()
        }

        fun cancelTransfer() {
            Bukkit.getScheduler().cancelTask(confirmationTaskId)
            confirmations.remove(sender.uniqueId)
            sender.sendMessage(getMessage("command_transfer_cancelled"))
            targetPlayer.sendMessage(getMessage("command_transfer_cancelled"))
        }
    }
}