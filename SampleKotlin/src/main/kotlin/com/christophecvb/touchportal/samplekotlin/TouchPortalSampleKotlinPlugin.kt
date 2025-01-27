/*
 * Touch Portal Plugin SDK
 *
 * Copyright 2020 Christophe Carvalho Vilas-Boas
 * christophe.carvalhovilasboas@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.christophecvb.touchportal.samplekotlin

import com.christophecvb.touchportal.TouchPortalPlugin
import com.christophecvb.touchportal.annotations.*
import com.christophecvb.touchportal.helpers.PluginHelper
import com.christophecvb.touchportal.helpers.ReceivedMessageHelper
import com.christophecvb.touchportal.model.*
import com.christophecvb.touchportal.samplekotlin.invokable.action.ExampleClassAction
import com.christophecvb.touchportal.samplekotlin.invokable.connector.ExampleClassConnector
import com.google.gson.JsonObject

import java.io.File
import java.util.logging.Logger
import kotlin.system.exitProcess

@Plugin(
    version = BuildConfig.VERSION_CODE,
    colorDark = "#203060",
    colorLight = "#4070F0",
    name = BuildConfig.NAME,
    parentCategory = ParentCategory.CONTENT
)
class TouchPortalSampleKotlinPlugin(parallelizeActions: Boolean) :
    TouchPortalPlugin(parallelizeActions), TouchPortalPlugin.TouchPortalPluginListener {

    companion object {
        /**
         * Logger used within the plugin
         */
        @JvmStatic
        private val LOGGER = Logger.getLogger(TouchPortalPlugin::class.java.name)

        @JvmStatic
        fun main(args: Array<String>) {
            if (args.size == 1) {
                if (PluginHelper.COMMAND_START == args[0]) {
                    // Initialize the Plugin
                    val touchPortalSampleKotlinPlugin = TouchPortalSampleKotlinPlugin(true)

                    // Register Invokable
                    touchPortalSampleKotlinPlugin.registerInvokable(TouchPortalSampleKotlinPluginConstants.BaseCategory.Actions.ExampleClassAction.ID, ExampleClassAction::class.java)
                    touchPortalSampleKotlinPlugin.registerInvokable(TouchPortalSampleKotlinPluginConstants.CategoryWithSubs.SubCat1.ID, ExampleClassConnector::class.java)

                    // Load a properties File
                    touchPortalSampleKotlinPlugin.loadProperties("plugin.config")

                    // Get a property
                    LOGGER.info { touchPortalSampleKotlinPlugin.getProperty("samplekey") }

                    // Set a property
                    touchPortalSampleKotlinPlugin.setProperty("samplekey", "Value set from Plugin")

                    // Store the properties
                    touchPortalSampleKotlinPlugin.storeProperties()

                    // Initiate the connection with the Touch Portal Plugin System
                    val connectedPairedAndListening = touchPortalSampleKotlinPlugin.connectThenPairAndListen(touchPortalSampleKotlinPlugin)

                    if (connectedPairedAndListening) {
                        LOGGER.info {
                            "Plugin with ID[${TouchPortalSampleKotlinPluginConstants.ID}] Connected and Paired!"
                        }

                        // Update a State with the ID from the Generated Constants Class
                        touchPortalSampleKotlinPlugin.sendStateUpdate(TouchPortalSampleKotlinPluginConstants.BaseCategory.States.CustomStateWithEvent.ID, "2")

                        // Create a new State
                        touchPortalSampleKotlinPlugin.sendCreateState("BaseCategory", "createdState1", "Created State 01", "${System.currentTimeMillis()}1")
                        touchPortalSampleKotlinPlugin.sendCreateState("BaseCategory", "createdState1", "Created State 01", "${System.currentTimeMillis()}2")
                        touchPortalSampleKotlinPlugin.sendCreateState("BaseCategory", "createdState2", "Created State 02", "${System.currentTimeMillis()}")
                        touchPortalSampleKotlinPlugin.sendRemoveState("BaseCategory", "createdState2")
                        touchPortalSampleKotlinPlugin.sendRemoveState("BaseCategory", "createdState1")
                        touchPortalSampleKotlinPlugin.sendRemoveState("BaseCategory", "customState")

                        // Update State Choice
                        touchPortalSampleKotlinPlugin.sendChoiceUpdate(TouchPortalSampleKotlinPluginConstants.SecondCategory.States.CustomStateChoice.ID, arrayOf<String>("1", "2", "3"))

                        // Send notification
                        touchPortalSampleKotlinPlugin.sendShowNotification(
                            TouchPortalSampleKotlinPluginConstants.BaseCategory.ID + ".exampleNotification",
                            "Example notification",
                            "the message of the notification",
                            arrayOf(
                                TPNotificationOption(TouchPortalSampleKotlinPluginConstants.BaseCategory.ID + ".exampleNotification.options.exampleOption", "example option")
                            )
                        )

                        // Update connector value
                        touchPortalSampleKotlinPlugin.sendConnectorUpdate(TouchPortalSampleKotlinPluginConstants.ID, TouchPortalSampleKotlinPluginConstants.BaseCategory.Connectors.ConnectorSimple.ID, 90, null)
                        try {
                            Thread.sleep(1000)
                        } catch (ignored: InterruptedException) {
                        }
                        touchPortalSampleKotlinPlugin.sendConnectorUpdate(TouchPortalSampleKotlinPluginConstants.ID, TouchPortalSampleKotlinPluginConstants.BaseCategory.Connectors.ConnectorSimple.ID, 50, null)

                        val states = HashMap<String, Any>()
                        states[TouchPortalSampleKotlinPluginConstants.BaseCategory.States.CustomStateWithEvent.ID] = "Value"
                        touchPortalSampleKotlinPlugin.sendTriggerEvent(TouchPortalSampleKotlinPluginConstants.BaseCategory.Events.CustomStateWithEvent.ID, states)
                    }
                }
            }
        }
    }


    enum class Categories {
        /**
         * Category definition example
         */
        @Category(name = "Touch Portal Plugin Example Base Category", imagePath = "images/icon-24.png")
        BaseCategory,
        @Category(name = "Touch Portal Plugin Example Second Category", imagePath = "images/icon-24.png")
        SecondCategory,
        @Category(name = "Touch Portal Plugin Example Category With Subcategories", imagePath = "images/icon-24.png",
            subCategories = [
                Category.SubCategory(id = "SubCat1", name = "SubCategory 1", imagePath = "%TP_PLUGIN_FOLDER%/images/icon-24.png")
            ])
        CategoryWithSubs
    }

    /* Settings Panel */

    /**
     * Setting of type text definition example
     */
    @Setting(name = "IP", defaultValue = "localhost", maxLength = 15.0, tooltip = Setting.Tooltip(
        title = "IP address",
        body = "ip address to connect to",
        docUrl = "https://example.com"
    ))
    private val ipSetting: String = "localhost"

    /**
     * Setting of type number definition example
     */
    @Setting(name = "Update Delay", defaultValue = "10", minValue = 10.0, maxValue = 30.0)
    private val updateDelaySetting = 10

    /**
     * Setting of type String and is read only definition example
     */
    @Setting(name = "Read Only", defaultValue = "Disconnected", isReadOnly = true)
    private val readOnly: String = "Disconnected"


    /* States and Events */

    /**
     * State and Event definition example
     */
    @State(defaultValue = "1", categoryId = "BaseCategory")
    @Event(valueChoices = ["1", "2"], format = "When customStateWithEvent becomes \$val")
    private val customStateWithEvent: String = "1"

    /**
     * State and Event definition example
     */
    @State(defaultValue = "1", categoryId = "BaseCategory")
    @Event(format = "When customStateWithEvent becomes \$val")
    private val stateWithEventTypeText: String = "1"

    /**
     * State and Event in Subcategory definition example
     */
    @State(defaultValue = "1", categoryId = "CategoryWithSubs")
    @Event(valueChoices = ["1", "2"], format = "When customStateWithEventInSubCat becomes \$val", subCategoryId = "Cat1")
    private val customStateWithEventInSubCat: String = "1"

    /**
     * State of type choice definition example
     */
    @State(valueChoices = ["1", "2"], defaultValue = "1", categoryId = "SecondCategory")
    private val customStateChoice: Array<String> = emptyArray()

    /**
     * State of type text definition example
     */
    @State(defaultValue = "Default Value", categoryId = "SecondCategory")
    private val customStateText: String = "Default Value"


    /* Actions */

    /**
     * Action example with no parameter
     */
    @Action(description = "Long Description of Action Simple", format = "Do a simple action", categoryId = "BaseCategory", name = "Action Simple")
    @ActionTranslation(language = Language.FRENCH, description = "Description longue de Action Simple", format = "Exécute une action simple", prefix = "Mon préfixe", name = "Action Simple")
    @ActionTranslation(language = Language.PORTUGUESE, description = "Descrição longa da Acção Simples", format = "Realiza uma acção simples", prefix = "Meu prefixo", name = "Acção Simples")
    private fun actionSimple() {
        LOGGER.info { "Action actionSimple received" }
    }

    /**
     * Action example with a Data Text parameter
     *
     * @param text String
     */
    @Action(description = "Long Description of Dummy Action with Data Text", format = "Set text to {\$text\$}", categoryId = "BaseCategory")
    fun actionWithText(@Data text: String) {
        LOGGER.info { "Action actionWithText received: $text" }
    }

    /**
     * Action example without Data but 1 parameter
     *
     * This action will not be called automatically by the SDK
     *
     * @param jsonAction JSONObject
     * @see TouchPortalSampleKotlinPlugin.onReceived
     */
    @Action(name = "Action without Data", description = "Long Description of Action without Data", categoryId = "BaseCategory")
    private fun actionWithoutData(jsonAction: JsonObject) {
        LOGGER.info { "Action actionWithoutData received [$jsonAction]" }
    }

    /**
     * Action example with a Data Choice
     *
     * @param doActions String[]
     */
    @Action(description = "Long Description of Action with Choice", format = "Do action {\$doActions\$}", categoryId = "BaseCategory")
    private fun actionWithChoice(@Data(valueChoices = ["Enable", "Disable", "Toggle"], defaultValue = "Toggle") doActions: Array<String>) {
        // The user selected value is passed at index 0
        LOGGER.info { "Action actionWithChoice received: ${doActions[0]}" }
    }

    /**
     * Action example with a Data Switch
     *
     * @param isOn boolean
     */
    @Action(description = "Long Description of Action with Switch", format = "Switch to {\$isOn\$}", categoryId = "BaseCategory")
    private fun actionWithSwitch(@Data(defaultValue = "false") isOn: Boolean) {
        LOGGER.info { "Action actionWithSwitch received: $isOn" }
    }

    /**
     * Action example with a Data File
     *
     * @param file File
     */
    @Action(description = "Long Description of Action with File", format = "Do an action with {\$file\$}", categoryId = "BaseCategory")
    private fun actionWithFile(@Data file: File) {
        LOGGER.info { "Action actionWithFile received: " + file.absolutePath }
    }

    /**
     * Action example with a Data File that is a directory
     *
     * @param directory File
     */
    @Action(description = "Long Description of Action with Directory", format = "Do an action with {\$directory\$}", categoryId = "BaseCategory")
    private fun actionWithDirectory(@Data(isDirectory = true) directory: File) {
        LOGGER.info { "Action actionWithDirectory received: ${directory.absolutePath}" }
    }

    /**
     * Action example with a Data Integer and Double
     *
     * @param integerValue Integer
     * @param doubleValue  Double
     */
    @Action(description = "Long Description of Action with Numbers", format = "Do an action with {\$integerValue\$} and {\$doubleValue\$}", categoryId = "BaseCategory")
    private fun actionWithNumbers(@Data(minValue = -1.0, maxValue = 42.0, defaultValue = "0") integerValue: Int, @Data doubleValue: Double) {
        LOGGER.info { "Action actionWithNumber received: $integerValue and $doubleValue" }
    }

    /**
     * Action example with a Data Color
     *
     * @param color String
     */
    @Action(description = "Long Description of Action with Color", format = "Do an action with {\$color\$}", categoryId = "BaseCategory")
    private fun actionWithColor(@Data(defaultValue = "#00000000", isColor = true) color: String) {
        LOGGER.info { "Action actionWithColor received: $color" }
    }

    @Action(name = "Hold Me!", hasHoldFunctionality = true, categoryId = "BaseCategory")
    private fun actionHoldable() {
        var isHeld = this.isActionBeingHeld(TouchPortalSampleKotlinPluginConstants.BaseCategory.Actions.ActionHoldable.ID)
        if (isHeld != null) {
            // Action is triggered by a Hold
            while (isHeld != null && isHeld) {
                LOGGER.info { "actionHoldable has been triggered by a HOLD" }
                try {
                    Thread.sleep(100)
                } catch (ignored: InterruptedException) {
                }
                isHeld = this.isActionBeingHeld(TouchPortalSampleKotlinPluginConstants.BaseCategory.Actions.ActionHoldable.ID)
            }
        } else {
            // Action is triggered by a Press
            LOGGER.info { "actionHoldable has been triggered by a Press" }
        }
    }

    @Action(format = "Do Action with Choice {\$choices\$}", categoryId = "CategoryWithSubs", subCategoryId = "SubCat1")
    private fun actionWithDataStateId(@Data(stateId = "customStateChoice") choices: Array<String>) {
        LOGGER.info { "Action with Data State Id received: ${choices[0]}" }
    }

    /**
     * Connector example with no parameter
     */
    @Connector(format = "Connector Simple", categoryId = "BaseCategory")
    private fun connectorSimple(@ConnectorValue value: Int) {
        LOGGER.info { String.format("Connector connectorSimple received: value[%d]", value) }
    }

    override fun onDisconnected(exception: Exception?) {
        // Socket connection is lost or plugin has received close message
        exitProcess(0)
    }

    override fun onReceived(jsonMessage: JsonObject) {
        LOGGER.info { "onReceived $jsonMessage" }
        // Check if ReceiveMessage is an Action
        if (ReceivedMessageHelper.isTypeAction(jsonMessage)) {
            // Get the Action ID
            val receivedActionId = ReceivedMessageHelper.getActionId(jsonMessage)
            if (receivedActionId != null) {
                when (receivedActionId) {
                    TouchPortalSampleKotlinPluginConstants.BaseCategory.Actions.ActionWithoutData.ID -> {
                        // Manually call the action method because the parameter jsonMessage is not annotated with @Data
                        this.actionWithoutData(jsonMessage)
                    }
                }
            }
        }
        // dummyWithDataText, dummyWithDataChoice, dummySwitchAction and dummyAction are automatically called by the SDK
    }

    override fun onInfo(tpInfoMessage: TPInfoMessage) {
        this.sendSettingUpdate(TouchPortalSampleKotlinPluginConstants.Settings.ReadOnly.NAME, "Connected at ${System.currentTimeMillis()}", false)
    }

    override fun onListChanged(tpListChangedMessage: TPListChangedMessage) {

        LOGGER.info { "onListChanged $tpListChangedMessage" }
    }

    override fun onBroadcast(tpBroadcastMessage: TPBroadcastMessage) {
        LOGGER.info { "onBroadcast $tpBroadcastMessage" }
    }

    override fun onSettings(tpSettingsMessage: TPSettingsMessage) {
        LOGGER.info { "onSettings ${tpSettingsMessage.settings}" }
    }

    override fun onNotificationOptionClicked(tpNotificationOptionClickedMessage: TPNotificationOptionClickedMessage) {
        if (tpNotificationOptionClickedMessage.notificationId == TouchPortalSampleKotlinPluginConstants.BaseCategory.ID + ".exampleNotification") {
            if (tpNotificationOptionClickedMessage.optionId == TouchPortalSampleKotlinPluginConstants.BaseCategory.ID + ".exampleNotification.options.exampleOption") {
                LOGGER.info { "Example option clicked" }
            }
        }
    }
}
