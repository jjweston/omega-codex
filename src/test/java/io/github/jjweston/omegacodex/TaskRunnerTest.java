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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
public class TaskRunnerTest
{
    @Mock private OmegaCodexUtil mockOmegaCodexUtil;

    private TaskRunner taskRunner;

    @BeforeEach
    void setUp()
    {
        this.taskRunner = new TaskRunner( 5_000, this.mockOmegaCodexUtil );
    }

    @Test
    void get_nullTaskName()
    {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> this.taskRunner.get( null, () -> null ));

        assertEquals( "Task name must not be null.", exception.getMessage() );
    }

    @Test
    void get_emptyTaskName()
    {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> this.taskRunner.get( "", () -> null ));

        assertEquals( "Task name must not be empty.", exception.getMessage() );
    }

    @Test
    void get_nullTask()
    {
        String taskName = "get_nullTask";

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> this.taskRunner.get( taskName, null ));

        assertEquals( "Task must not be null.", exception.getMessage() );
    }

    @Test
    void get_taskInterrupted()
    {
        String taskName = "get_interruptedException";

        OmegaCodexException exception = assertThrowsExactly(
                OmegaCodexException.class,
                () -> this.taskRunner.get( taskName, () -> { throw new InterruptedException(); } ));

        assertEquals( taskName + ", Task Interrupted", exception.getMessage() );
        verify( this.mockOmegaCodexUtil ).interruptThread();
    }

    @Test
    void get_omegaCodexException()
    {
        String taskName = "get_omegaCodexException";
        String message = "Test Message";

        OmegaCodexException exception = assertThrowsExactly(
                OmegaCodexException.class,
                () -> this.taskRunner.get( taskName, () -> { throw new OmegaCodexException( message ); } ));

        assertEquals( message, exception.getMessage() );
    }

    @Test
    void get_exception()
    {
        String taskName = "get_exception";
        Exception innerException = new Exception( "Inner Exception" );

        OmegaCodexException exception = assertThrowsExactly(
                OmegaCodexException.class,
                () -> this.taskRunner.get( taskName, () -> { throw innerException; } ));

        assertEquals( taskName + ", Exception Occurred", exception.getMessage() );
        assertEquals( innerException, exception.getCause() );
    }

    @Test
    void get_success_startMessage_rateLimited_sleepInterrupted() throws Exception
    {
        String taskName = "get_success_startMessage_rateLimited_sleepInterrupted";

        when( this.mockOmegaCodexUtil.nanoTime() )
                .thenReturn(  1_000_000_000L )  // start #1 (first init is skipped)
                .thenReturn(  2_250_000_000L )  // stop  #1
                .thenReturn( 10_000_000_000L )  // init  #2
                .thenReturn( 10_000_000_000L )  // start #2
                .thenReturn( 11_500_000_000L )  // stop  #2
                .thenReturn( 20_000_000_000L )  // init  #3
                .thenReturn( 20_000_000_000L )  // start #3
                .thenReturn( 21_750_000_000L )  // stop  #3
                .thenReturn( 22_500_000_000L ); // init  #4 (final start and end are skipped)

        doThrow( new InterruptedException() ).when( this.mockOmegaCodexUtil ).sleepThread( 2_500 );

        assertEquals( 42, this.taskRunner.get( taskName, null,            () -> 42 ));
        assertEquals( 43, this.taskRunner.get( taskName, "",              () -> 43 ));
        assertEquals( 44, this.taskRunner.get( taskName, "Start Message", () -> 44 ));

        OmegaCodexException exception = assertThrowsExactly(
                OmegaCodexException.class, () -> this.taskRunner.get( taskName, () -> null ));

        assertEquals( taskName + ", Sleep Interrupted", exception.getMessage() );

        InOrder inOrder = inOrder( this.mockOmegaCodexUtil );
        inOrder.verify( this.mockOmegaCodexUtil ).println( taskName + ", Starting" );
        inOrder.verify( this.mockOmegaCodexUtil ).println( taskName + ", Complete, Duration: 1,250 ms" );
        inOrder.verify( this.mockOmegaCodexUtil ).println( taskName + ", Starting" );
        inOrder.verify( this.mockOmegaCodexUtil ).println( taskName + ", Complete, Duration: 1,500 ms" );
        inOrder.verify( this.mockOmegaCodexUtil ).println( taskName + ", Starting, Start Message" );
        inOrder.verify( this.mockOmegaCodexUtil ).println( taskName + ", Complete, Duration: 1,750 ms" );
        inOrder.verify( this.mockOmegaCodexUtil ).println( taskName + ", Sleeping, Duration: 2,500 ms" );
        inOrder.verify( this.mockOmegaCodexUtil ).interruptThread();
        inOrder.verifyNoMoreInteractions();
    }
}
