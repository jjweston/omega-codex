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

record StringSegment( int segmentStart, int segmentEnd, int contentEnd,
                      int startLine, int startColumn, int endLine, int endColumn )
{
    StringSegment
    {
        if ( segmentStart < 0 )
            throw new IllegalArgumentException( "Segment Start must be greater than or equal to zero." );
        if ( segmentEnd <= segmentStart )
            throw new IllegalArgumentException( "Segment End must be greater than Segment Start." );
        if ( contentEnd < segmentStart )
            throw new IllegalArgumentException( "Content End must be greater than or equal to Segment Start." );
        if ( contentEnd > segmentEnd )
            throw new IllegalArgumentException( "Content End must be less than Segment End." );
        if ( startLine < 0 )
            throw new IllegalArgumentException( "Start Line must be greater than or equal to zero." );
        if ( startColumn < 0 )
            throw new IllegalArgumentException( "Start Column must be greater than or equal to zero." );
        if ( endLine < startLine )
            throw new IllegalArgumentException( "End Line must be greater than or equal to Start Line." );
        if ( endColumn < 0 )
            throw new IllegalArgumentException( "End Column must be greater than or equal to zero." );
        if (( startLine == endLine ) && ( endColumn < startColumn ))
            throw new IllegalArgumentException( "When Start Line and End Line are equal, " +
                                                "End Column must be greater than Start Column." );
    }
}
