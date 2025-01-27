package com.christophecvb.touchportal.samplekotlin.invokable.action

import com.christophecvb.touchportal.TPAction
import com.christophecvb.touchportal.TouchPortalPlugin
import com.christophecvb.touchportal.annotations.Action
import com.christophecvb.touchportal.annotations.ActionTranslation
import com.christophecvb.touchportal.annotations.Data
import com.christophecvb.touchportal.annotations.Language
import com.christophecvb.touchportal.model.TPListChangedMessage
import com.christophecvb.touchportal.samplekotlin.TouchPortalSampleKotlinPlugin

import java.util.logging.Logger

@Action(
    name = "Example Class Action",
    format = "Example Class Action with param {\$param\$}",
    categoryId = "BaseCategory"
)
@ActionTranslation(
    language = Language.FRENCH,
    name = "Exemple d'Action via une Classe",
    format = "Exemple d'Action via une Classe avec le paramètre {\$param\$}"
)
class ExampleClassAction(touchPortalPlugin: TouchPortalSampleKotlinPlugin?) :
    TPAction<TouchPortalSampleKotlinPlugin?>(touchPortalPlugin) {

    companion object {
        private val LOGGER: Logger = Logger.getLogger(TouchPortalPlugin::class.java.name)
    }

    @Data
    private val param: String? = null

    override fun onInvoke() {
        LOGGER.info("ExampleClassAction.onInvoke this.param=" + this.param)
    }

    override fun onListChanged(tpListChangedMessage: TPListChangedMessage) {
        LOGGER.info("ExampleClassAction.onListChanged")
    }
}
