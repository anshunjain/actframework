package act.sys;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.Act;
import org.osgl.util.C;
import org.osgl.util.OS;
import org.osgl.util.S;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.AnnotatedElement;

/**
 * Mark a module should only be loaded in certain environment
 */
public final class Env {

    private Env() {}

    /**
     * Used to mark a dependency injector module that
     * should be load only in specified profile.
     *
     * This annotation shall NOT used along with
     * {@link Mode} and {@link Group}
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Profile {

        /**
         * The profile specification
         */
        String value();

        /**
         * If unless is `true` then the module should be load
         * unless the current profile is the value specified
         */
        boolean unless() default false;
    }

    /**
     * Used to mark a dependency injector module that
     * should be load only in specified node group
     *
     * This annotation shall NOT used along with
     * {@link Mode} and {@link Profile}
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Group {

        /**
         * The node group specification
         */
        String value();

        /**
         * If unless is `true` then the module should be load
         * unless the current node group is the value specified
         */
        boolean unless() default false;
    }

    /**
     * Used to mark a dependency injector module
     * that should be load only in specified mode
     *
     * This annotation shall NOT used along with
     * {@link Profile} and {@link Group}
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Mode {

        /**
         * The mode specification
         */
        Act.Mode value();

        /**
         * If unless is `true` then the module should be load
         * unless the current mode is the value specified
         */
        boolean unless() default false;
    }

    public static boolean matches(Mode modeTag) {
        return modeMatches(modeTag.value(), modeTag.unless());
    }

    public static boolean modeMatches(Act.Mode mode) {
        return mode == Act.mode();
    }

    public static boolean modeMatches(Act.Mode mode, boolean unless) {
        return unless ^ modeMatches(mode);
    }

    public static boolean matches(Profile profileTag) {
        return profileMatches(profileTag.value(), profileTag.unless());
    }

    public static boolean profileMatches(String profile) {
        return S.eq(profile, Act.profile(), S.IGNORECASE);
    }

    public static boolean profileMatches(String profile, boolean unless) {
        return unless ^ profileMatches(profile);
    }

    public static boolean matches(Group groupTag) {
        return groupMatches(groupTag.value(), groupTag.unless());
    }

    public static boolean groupMatches(String group) {
        return S.eq(group, Act.nodeGroup(), S.IGNORECASE);
    }

    public static boolean groupMatches(String group, boolean unless) {
        return unless ^ groupMatches(group);
    }

    private static final C.Set<Class<? extends Annotation>> ENV_ANNOTATION_TYPES = C.set(
            Env.Mode.class, Env.Profile.class, Env.Group.class
    );

    public static boolean isEnvAnnotation(Class<? extends Annotation> type) {
        return ENV_ANNOTATION_TYPES.contains(type);
    }

    /**
     * Determine if an {@link AnnotatedElement} has environment annotations and if it has then check
     * if all environment annotations matches the current executing environment
     * @param annotatedElement an annotated element
     * @return `true` if the element does not have environment annotations or all environment annotations matches
     */
    public static boolean matches(AnnotatedElement annotatedElement) {
        Annotation[] annotations = annotatedElement.getDeclaredAnnotations();
        for (Annotation anno : annotations) {
            if (anno instanceof Profile) {
                Profile profile = (Profile)anno;
                if (!matches(profile)) {
                    return false;
                }
            } else if (anno instanceof Group) {
                Group group = (Group) anno;
                if (!matches(group)) {
                    return false;
                }
            } else if (anno instanceof Mode) {
                Mode mode = (Mode) anno;
                if (!matches(mode)) {
                    return false;
                }
            }
        }
        return true;
    }

    // See http://stackoverflow.com/questions/534648/how-to-daemonize-a-java-program
    public static class PID {

        private static String pid = getPid();

        private static String getPid() {
            OS os = OS.get();
            if (os.isUnix()) {
                File proc_self = new File("/proc/self");
                if(proc_self.exists()) try {
                    return proc_self.getCanonicalFile().getName();
                }
                catch(Exception e) {
                    /// Continue on fall-back
                }
                File bash = new File("/bin/sh");
                if(bash.exists()) {
                    ProcessBuilder pb = new ProcessBuilder("/bin/sh","-c","echo $PPID");
                    try {
                        Process p = pb.start();
                        BufferedReader rd = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        return rd.readLine();
                    }
                    catch(IOException e) {
                        return String.valueOf(Thread.currentThread().getId());
                    }
                }
            } else {
                String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
                if (null != nameOfRunningVM) {
                    int p = nameOfRunningVM.indexOf('@');
                    if (p > -1) {
                        return nameOfRunningVM.substring(0, p);
                    }
                }
            }
            // The final resort
            return String.valueOf(Thread.currentThread().getId());
        }

        public static String get() {
            return pid;
        }
    }

}
