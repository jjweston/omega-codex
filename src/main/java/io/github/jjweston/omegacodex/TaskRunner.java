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

class TaskRunner
{
    @FunctionalInterface interface ThrowingRunnable { void run() throws Exception; }
    @FunctionalInterface interface ThrowingSupplier< T > { T get() throws Exception; }

    private final long           rateLimitDelay;
    private final OmegaCodexUtil omegaCodexUtil;

    private boolean runPreviously = false;
    private long    previousStart;

    TaskRunner( long rateLimitDelay )
    {
        this.rateLimitDelay = rateLimitDelay;
        this.omegaCodexUtil = new OmegaCodexUtil();
    }

    TaskRunner( long rateLimitDelay, OmegaCodexUtil omegaCodexUtil )
    {
        this.rateLimitDelay = rateLimitDelay;
        this.omegaCodexUtil = omegaCodexUtil;
    }

    < T > T get( String taskName, ThrowingSupplier< T > task )
    {
        return this.get( taskName, null, task );
    }

    < T > T get( String taskName, String startMessage, ThrowingSupplier< T > task )
    {
        if ( taskName == null ) throw new IllegalArgumentException( "Task name must not be null." );
        if ( taskName.isEmpty() ) throw new IllegalArgumentException( "Task name must not be empty." );
        if ( task == null ) throw new IllegalArgumentException( "Task must not be null." );

        if ( this.runPreviously )
        {
            long initTime = this.omegaCodexUtil.nanoTime();
            long previousDeltaMs = ( initTime - this.previousStart ) / 1_000_000;
            long delayMs = this.rateLimitDelay - previousDeltaMs;

            if ( delayMs > 0 )
            {
                this.omegaCodexUtil.println( String.format( taskName + ", Sleeping, Duration: %,d ms", delayMs ));
                try { this.omegaCodexUtil.sleepThread( delayMs ); }
                catch ( InterruptedException e )
                {
                    this.omegaCodexUtil.interruptThread();
                    throw new OmegaCodexException( taskName + ", Sleep Interrupted", e );
                }
            }
        }
        else this.runPreviously = true;

        String message = taskName + ", Starting";
        if (( startMessage != null ) && ( !startMessage.isEmpty() )) message += ", " + startMessage;

        this.omegaCodexUtil.println( message );
        long startTime = this.omegaCodexUtil.nanoTime();
        this.previousStart = startTime;

        T result;
        try { result = task.get(); }
        catch ( InterruptedException e )
        {
            this.omegaCodexUtil.interruptThread();
            throw new OmegaCodexException( taskName + ", Task Interrupted", e );
        }
        catch ( OmegaCodexException e ) { throw e; }
        catch ( Exception e ) { throw new OmegaCodexException( taskName + ", Exception Occurred", e ); }

        long stopTime = this.omegaCodexUtil.nanoTime();
        long deltaMs = ( stopTime - startTime ) / 1_000_000;
        this.omegaCodexUtil.println( String.format( taskName + ", Complete, Duration: %,d ms", deltaMs ));

        return result;
    }

    void run( String taskName, ThrowingRunnable task )
    {
        this.get( taskName, () -> { task.run(); return null; } );
    }
}
