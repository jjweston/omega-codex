/*

Copyright 2025 Jeffrey J. Weston <jjweston@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/

package io.github.jjweston.omegacodex;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Split
{
    private Split() {}

    public static void main( String[] args )
    {
        Path inputFilePath = Paths.get( "readme.md" );
        MarkdownSplitter markdownSplitter = new MarkdownSplitter();
        List< String > chunks = markdownSplitter.split( inputFilePath );

        int index = 0;
        for ( String chunk : chunks )
        {
            System.out.println( "-------------------- Chunk " + ++index + " --------------------" );
            System.out.println();
            System.out.println( chunk );
        }
    }
}
