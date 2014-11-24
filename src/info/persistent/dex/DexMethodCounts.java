/*
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
 */

package info.persistent.dex;

import java.io.PrintStream;
import java.util.*;

import com.android.dexdeps.ClassRef;
import com.android.dexdeps.DexData;
import com.android.dexdeps.MethodRef;
import com.android.dexdeps.Output;

public class DexMethodCounts {
    private static final PrintStream out = System.out;
    public static int overallCount = 0;

    private static HashMap<String, Integer> sMap = new HashMap<String, Integer>();

    public static void generate(DexData dexData, boolean includeClasses, String packageFilter,
            int maxDepth, Filter filter) {
        MethodRef[] methodRefs = getMethodRefs(dexData, filter);
        Node packageTree = new Node();

        for (MethodRef methodRef : methodRefs) {
            String classDescriptor = methodRef.getDeclClassName();
            String packageName = includeClasses ? Output.descriptorToDot(classDescriptor).replace(
                '$', '.') : Output.packageNameOnly(classDescriptor);
            if (packageFilter != null && !packageName.startsWith(packageFilter)) {
                continue;
            }
            String packageNamePieces[] = packageName.split("\\.");
            Node packageNode = packageTree;
            for (int i = 0; i < packageNamePieces.length && i < maxDepth; i++) {
                packageNode.count++;
                String name = packageNamePieces[i];
                if (packageNode.children.containsKey(name)) {
                    packageNode = packageNode.children.get(name);
                } else {
                    Node childPackageNode = new Node();
                    packageNode.children.put(name, childPackageNode);
                    packageNode = childPackageNode;
                }
            }
            packageNode.count++;
        }

        packageTree.output("");
    }

    public static void generateSortedList() {
        List<Map.Entry<String, Integer>> infoIds = new ArrayList<Map.Entry<String, Integer>>(
                sMap.entrySet());

        Collections.sort(infoIds, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        for (int i = 0, size = infoIds.size(); i < size; i++) {
            System.out.println("package = " + infoIds.get(i).getKey() + " , method count = "
                    + infoIds.get(i).getValue());
        }
    }

    private static MethodRef[] getMethodRefs(DexData dexData, Filter filter) {
        MethodRef[] methodRefs = dexData.getMethodRefs();
        out.println("Read in " + methodRefs.length + " method IDs.");
        if (filter == Filter.ALL) {
            return methodRefs;
        }

        ClassRef[] externalClassRefs = dexData.getExternalReferences();
        out.println("Read in " + externalClassRefs.length + " external class references.");
        Set<MethodRef> externalMethodRefs = new HashSet<MethodRef>();
        for (ClassRef classRef : externalClassRefs) {
            for (MethodRef methodRef : classRef.getMethodArray()) {
                externalMethodRefs.add(methodRef);
            }
        }
        out.println("Read in " + externalMethodRefs.size() + " external method references.");
        List<MethodRef> filteredMethodRefs = new ArrayList<MethodRef>();
        for (MethodRef methodRef : methodRefs) {
            boolean isExternal = externalMethodRefs.contains(methodRef);
            if ((filter == Filter.DEFINED_ONLY && !isExternal)
                    || (filter == Filter.REFERENCED_ONLY && isExternal)) {
                filteredMethodRefs.add(methodRef);
            }
        }
        out.println("Filtered to " + filteredMethodRefs.size() + " "
                + (filter == Filter.DEFINED_ONLY ? "defined" : "referenced") + " method IDs.");
        return filteredMethodRefs.toArray(new MethodRef[filteredMethodRefs.size()]);
    }

    enum Filter {
        ALL, DEFINED_ONLY, REFERENCED_ONLY
    }

    private static class Node {
        int count = 0;
        NavigableMap<String, Node> children = new TreeMap<String, Node>();

        void output(String indent) {
            if (indent.length() == 0) {
                out.println("<root>: " + count);
                overallCount += count;
            }
            indent += "    ";
            for (String name : children.navigableKeySet()) {
                Node child = children.get(name);
                // Hack For DP
                if ("main".equals(name) || "wed".equals(name) || "wedding".equals(name)
                        || "t".equals(name) || "beauty".equals(name)) {
                    sMap.put(name, child.count);
                }
                out.println(indent + name + ": " + child.count);
                child.output(indent);
            }
        }
    }
}
