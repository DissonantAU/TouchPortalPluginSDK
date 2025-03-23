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

package com.christophecvb.touchportal.annotations.processor;

import com.christophecvb.touchportal.annotations.*;
import com.christophecvb.touchportal.annotations.processor.utils.Pair;
import com.christophecvb.touchportal.annotations.processor.utils.SpecUtils;
import com.christophecvb.touchportal.helpers.*;
import com.google.auto.service.AutoService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Touch Portal Plugin Annotations Processor
 */
@AutoService(Processor.class)
public class TouchPortalPluginAnnotationsProcessor extends AbstractProcessor {
    private Filer filer;
    private Messager messager;

    public Messager getMessager() {
        return this.messager;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();

        checkEnvironmentVariables(processingEnv.getOptions());
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Plugin.class.getCanonicalName());
        annotations.add(Setting.class.getCanonicalName());
        annotations.add(Category.class.getCanonicalName());
        annotations.add(Action.class.getCanonicalName());
        annotations.add(ActionTranslation.class.getCanonicalName());
        annotations.add(ActionTranslations.class.getCanonicalName());
        annotations.add(Data.class.getCanonicalName());
        annotations.add(State.class.getCanonicalName());
        annotations.add(Event.class.getCanonicalName());
        annotations.add(Connector.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver() || annotations.size() == 0) {
            return false;
        }
        this.messager.printMessage(Diagnostic.Kind.NOTE, this.getClass().getSimpleName() + ".process");

        try {
            Set<? extends Element> plugins = roundEnv.getElementsAnnotatedWith(Plugin.class);
            if (plugins.size() != 1) {
                throw new TPAnnotationException.Builder(Plugin.class).count(plugins.size()).build();
            }
            for (Element pluginElement : plugins) {
                Pair<JsonObject, TypeSpec.Builder> pluginPair = PluginProcessor.process(this, roundEnv, pluginElement);

                String entryFileName = "resources/" + PluginHelper.ENTRY_TP;
                FileObject actionFileObject = this.filer.createResource(StandardLocation.SOURCE_OUTPUT, "", entryFileName, pluginElement);
                Writer writer = actionFileObject.openWriter();
                writer.write(pluginPair.first.toString());
                writer.flush();
                writer.close();

                TypeSpec pluginTypeSpec = pluginPair.second.build();
                String packageName = ((PackageElement) pluginElement.getEnclosingElement()).getQualifiedName().toString();
                JavaFile javaConstantsFile = JavaFile.builder(packageName, pluginTypeSpec).build();
                javaConstantsFile.writeTo(this.filer);
            }
        }
        catch (Exception exception) {
            this.messager.printMessage(Diagnostic.Kind.ERROR, exception.getMessage());
        }

        return true;
    }

    // All JRE Options
    // For Compat with 8.3
    private final String optTPJreBundled = "tp.entry.startcmd.jre.bundled";

    private final String optTPJreAllOptimised = "tp.entry.startcmd.jre.all.optimised";
    private final String optTPJreAllBundled = "tp.entry.startcmd.jre.all.bundled";
    private final String optTPJreAllExternal = "tp.entry.startcmd.jre.all.external";

    private final String optTPJreWinDefault = "tp.entry.startcmd.jre.win.default";
    private final String optTPJreWinOptimised = "tp.entry.startcmd.jre.win.optimised";
    private final String optTPJreWinBundled = "tp.entry.startcmd.jre.win.bundled";
    private final String optTPJreWinExternal = "tp.entry.startcmd.jre.win.external";

    private final String optTPJreMacDefault = "tp.entry.startcmd.jre.mac.default";
    private final String optTPJreMacOptimised = "tp.entry.startcmd.jre.mac.optimised";
    private final String optTPJreMacBundled = "tp.entry.startcmd.jre.mac.bundled";
    private final String optTPJreMacExternal = "tp.entry.startcmd.jre.mac.external";

    private final String optTPJreLinuxDefault = "tp.entry.startcmd.jre.linux.default";
    private final String optTPJreLinuxOptimised = "tp.entry.startcmd.jre.linux.optimised";
    private final String optTPJreLinuxBundled = "tp.entry.startcmd.jre.linux.bundled";
    private final String optTPJreLinuxExternal = "tp.entry.startcmd.jre.linux.external";


    @Override
    public Set<String> getSupportedOptions() {
        Set<String> supportedOptions = new LinkedHashSet<>();

        supportedOptions.add(optTPJreBundled);

        supportedOptions.add(optTPJreAllOptimised);
        supportedOptions.add(optTPJreAllBundled);
        supportedOptions.add(optTPJreAllExternal);

        supportedOptions.add(optTPJreWinDefault);
        supportedOptions.add(optTPJreWinOptimised);
        supportedOptions.add(optTPJreWinBundled);
        supportedOptions.add(optTPJreWinExternal);

        supportedOptions.add(optTPJreMacDefault);
        supportedOptions.add(optTPJreMacOptimised);
        supportedOptions.add(optTPJreMacBundled);
        supportedOptions.add(optTPJreMacExternal);

        supportedOptions.add(optTPJreLinuxDefault);
        supportedOptions.add(optTPJreLinuxOptimised);
        supportedOptions.add(optTPJreLinuxBundled);
        supportedOptions.add(optTPJreLinuxExternal);

        return supportedOptions;
    }

    // Specify Defaults for 'Optimised'
    // As of Touch Portal 4.4 Build 2 Bundled TP Path doesn't work for Mac & Linux
    private final boolean optimisedJREWin = true;
    private final boolean optimisedJREMac = false;
    private final boolean optimisedJRELinux = false;

    // Default is All External
    private boolean useTPBundledJreWin = false;
    private boolean useTPBundledJreMac = false;
    private boolean useTPBundledJreLinux = false;

    public boolean isUseTPBundledJreWin() {
        return useTPBundledJreWin;
    }

    public boolean isUseTPBundledJreMac() {
        return useTPBundledJreMac;
    }

    public boolean isUseTPBundledJreLinux() {
        return useTPBundledJreLinux;
    }

    private void checkEnvironmentVariables(Map<String, String> processingEnvOptions) {
        if (processingEnvOptions != null && !processingEnvOptions.isEmpty()) {
            //this.messager.printMessage(Diagnostic.Kind.NOTE, this.getClass().getSimpleName() + ".checkEnvironmentVariables");

            boolean relevantOptionFound = false;

            for (String optionVal : processingEnvOptions.keySet()) {
                if (optionVal.startsWith("tp.")) {
                    relevantOptionFound = true;
                    break;
                    //this.messager.printMessage(Diagnostic.Kind.NOTE, "Relevant argument found: '" + optionVal + "'");
                }
            }

            if (relevantOptionFound) {
                if (processingEnvOptions.containsKey(optTPJreAllOptimised)) {
                    // Optimised is currently Windows Bundled, MAC and Linux External
                    // TP on Mac may have issues with running plugins with bundled JRE
                    useTPBundledJreWin = optimisedJREWin;
                    useTPBundledJreMac = optimisedJREMac;
                    useTPBundledJreLinux = optimisedJRELinux;
                }
                if (processingEnvOptions.containsKey(optTPJreAllExternal)) {
                    useTPBundledJreWin = false;
                    useTPBundledJreMac = false;
                    useTPBundledJreLinux = false;
                }
                if (processingEnvOptions.containsKey(optTPJreAllBundled) || processingEnvOptions.containsKey(optTPJreBundled)) {
                    useTPBundledJreWin = true;
                    useTPBundledJreMac = true;
                    useTPBundledJreLinux = true;
                }

                if (processingEnvOptions.containsKey(optTPJreWinDefault)) {
                    useTPBundledJreWin = false;
                }
                if (processingEnvOptions.containsKey(optTPJreWinOptimised)) {
                    useTPBundledJreWin = optimisedJREWin;
                }
                if (processingEnvOptions.containsKey(optTPJreWinBundled)) {
                    useTPBundledJreWin = true;
                }
                if (processingEnvOptions.containsKey(optTPJreWinExternal)) {
                    useTPBundledJreWin = false;
                }

                if (processingEnvOptions.containsKey(optTPJreMacDefault)) {
                    useTPBundledJreMac = false;
                }
                if (processingEnvOptions.containsKey(optTPJreMacOptimised)) {
                    useTPBundledJreMac = optimisedJREMac;
                }
                if (processingEnvOptions.containsKey(optTPJreMacBundled)) {
                    useTPBundledJreMac = true;
                }
                if (processingEnvOptions.containsKey(optTPJreMacExternal)) {
                    useTPBundledJreMac = false;
                }

                if (processingEnvOptions.containsKey(optTPJreLinuxDefault)) {
                    useTPBundledJreLinux = false;
                }
                if (processingEnvOptions.containsKey(optTPJreLinuxOptimised)) {
                    useTPBundledJreLinux = optimisedJRELinux;
                }
                if (processingEnvOptions.containsKey(optTPJreLinuxBundled)) {
                    useTPBundledJreLinux = true;
                }
                if (processingEnvOptions.containsKey(optTPJreLinuxExternal)) {
                    useTPBundledJreLinux = false;
                }
            }// else {
            //    this.messager.printMessage(Diagnostic.Kind.NOTE, this.getClass().getSimpleName() + ".checkEnvironmentVariables - no relevant options passed to the annotation processing tool.");
            //}
        }// else {
        //    this.messager.printMessage(Diagnostic.Kind.NOTE, this.getClass().getSimpleName() + ".checkEnvironmentVariables - no processor-specific options passed to the annotation processing tool.");
        //}
        //this.messager.printMessage(Diagnostic.Kind.NOTE, this.getClass().getSimpleName() + ".checkEnvironmentVariables result: TPBundledJREWin=" + useTPBundledJreWin + "; TPBundledJREMac=" + useTPBundledJreMac + "; TPBundledJRELinux=" + useTPBundledJreLinux);
    }
}
