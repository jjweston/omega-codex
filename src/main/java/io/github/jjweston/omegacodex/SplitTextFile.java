/*

Copyright 2026 Jeffrey J. Weston <jjweston@gmail.com>

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SplitTextFile
{
    private SplitTextFile() {}

    static void main( String[] args )
    {
        System.out.println( "Split Text File" );

        for ( String arg : args )
        {
            System.out.println( arg );
        }

        if ( args.length < 1 )
        {
            System.err.println( "Error: Filename not specified." );
            System.err.println( "Usage: mvn exec:exec -P text-split -D exec.filename=[filename]" );
            System.err.println( "Replace [filename] with the name of the text split to split." );
            System.exit( 1 );
        }

        Path path = Paths.get( args[ 0 ] );
        if ( !path.toFile().exists() ) throw new IllegalArgumentException( "File not found: " + path );

        String content;
        try { content = Files.readString( path ); }
        catch ( IOException e ) { throw new OmegaCodexException( "Exception reading file: " + path, e ); }

        List< StringSegment > stringSegments = SplitTextFile.getLines( content );
        String format = SplitTextFile.getStringSegmentFormat( stringSegments );

        int index = 0;
        for ( StringSegment stringSegment : stringSegments )
        {
            String stringSegmentContent = content.substring( stringSegment.segmentStart(), stringSegment.contentEnd() );
            if ( stringSegmentContent.length() > 100 )
                stringSegmentContent = stringSegmentContent.substring( 0, 100 ) + "...";

            System.out.format(
                    format, index++,
                    stringSegment.segmentStart(), stringSegment.segmentEnd(), stringSegment.contentEnd(),
                    stringSegment.startLine(), stringSegment.startColumn(),
                    stringSegment.endLine(), stringSegment.endColumn(),
                    stringSegmentContent );
        }
    }

    private static List< StringSegment > getLines( String content )
    {
        LinkedList< StringSegment > result = new LinkedList<>();

        int index = 0;
        Matcher matcher = Pattern.compile( "(.*)(\\R|\\z)" ).matcher( content );
        while ( matcher.find() )
        {
            if ( matcher.start() == matcher.end() ) break;
            result.add( new StringSegment( matcher.start(), matcher.end(), matcher.end( 1 ), index, 0, index + 1, 0 ));
            index++;
        }

        return result;
    }

    private static String getStringSegmentFormat( List< StringSegment > stringSegments )
    {
        StringSegment lastStringSegment = stringSegments.getLast();

        int maxIndexLength        = String.format( "%,d", stringSegments.size() - 1        ).length();
        int maxSegmentStartLength = String.format( "%,d", lastStringSegment.segmentStart() ).length();
        int maxSegmentEndLength   = String.format( "%,d", lastStringSegment.segmentEnd()   ).length();
        int maxContentEndLength   = String.format( "%,d", lastStringSegment.contentEnd()   ).length();
        int maxStartLineLength    = String.format( "%,d", lastStringSegment.startLine()    ).length();
        int maxStartColumnLength  = String.format( "%,d", lastStringSegment.startColumn()  ).length();
        int maxEndLineLength      = String.format( "%,d", lastStringSegment.endLine()      ).length();
        int maxEndColumnLength    = String.format( "%,d", lastStringSegment.endColumn()    ).length();

        return "Index"         + ": %," + maxIndexLength        + "d, " +
               "Segment Start" + ": %," + maxSegmentStartLength + "d, " +
               "Segment End"   + ": %," + maxSegmentEndLength   + "d, " +
               "Content End"   + ": %," + maxContentEndLength   + "d, " +
               "Start Line"    + ": %," + maxStartLineLength    + "d, " +
               "Start Column"  + ": %," + maxStartColumnLength  + "d, " +
               "End Line"      + ": %," + maxEndLineLength      + "d, " +
               "End Column"    + ": %," + maxEndColumnLength    + "d, " +
               "Content: %s%n";
    }
}
