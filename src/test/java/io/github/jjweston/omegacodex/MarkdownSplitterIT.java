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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MarkdownSplitterIT
{
    @Test
    void testSplit( @TempDir Path tempDir ) throws Exception
    {
        List< String > expectedChunks = new LinkedList<>();

        expectedChunks.add( """
                # Test Markdown

                This is a test Markdown file.
                """ );

        expectedChunks.add( """
                ## License

                ```text
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
                ```
                """ );

        String resourceFileName = this.getClass().getSimpleName() + ".md";
        Path resourcePath = tempDir.resolve( resourceFileName );

        try ( InputStream resourceStream = this.getClass().getResourceAsStream( resourceFileName ))
        {
            assertNotNull( resourceStream, "Missing Resource: " + resourceFileName );
            Files.copy( resourceStream, resourcePath );
        }

        MarkdownSplitter markdownSplitter = new MarkdownSplitter();
        List< String > actualChunks = markdownSplitter.split( resourcePath );
        assertThat( actualChunks ).as( "Chunks" ).isEqualTo( expectedChunks );
    }
}
