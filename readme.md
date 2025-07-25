# Omega Codex

Omega Codex is an AI-powered assistant that helps you explore, understand, and develop software projects.
It aims to support software developers by
automating tedious tasks,
answering project-specific questions, and
contributing meaningfully to the development process,
all while integrating tightly with version control and issue tracking platforms like GitHub.

## Early Development

> [!WARNING]
> Omega Codex is in early development and should be considered a prototype or proof of concept.
> It is not yet ready for general use.
> Expect incomplete features, experimental ideas, and frequent iteration.

The current early development goal for Omega Codex is to develop a minimal functional implementation
that is able to ingest basic information about a GitHub project into a vectorized knowledge base,
allow a user to make a query about the project,
use Retrieval Augmented Generation to send the user’s query
along with relevant portions of the project to the ChatGPT API,
and display the ChatGPT API response to the user.

## Development Process

Although Omega Codex is currently a one-person project,
it is being developed using a collaborative, team-oriented workflow.
Each feature or idea is tracked as a GitHub issue,
and changes are implemented through topic branches and pull requests.
This workflow has been intentionally chosen to:

- Generate realistic issue and pull request data that Omega Codex will eventually analyze and learn from.
- Simulate real-world team collaboration, enabling more robust development and testing of Omega Codex's capabilities.
- Explore and validate how Omega Codex can be used in complex projects with structured workflows.

## Configuration

To use Omega Codex you'll need an OpenAI API key.

If you use an API key with *Restricted* permissions
you must ensure that your API key has *Request* permission to the *Model capabilities* resource.
This is required for calling the *embeddings* API endpoint.

Once you have your OpenAI API key you must configure it in Omega Codex
by creating a file called `.env` in your project root directory with the following:

```env
OMEGACODEX_OPENAI_API_KEY=your-api-key-here
```

> [!CAUTION]
> Do not commit your `.env` file.
> It is already included in `.gitignore` to prevent accidental exposure of sensitive information.

## Prerequisites

Omega Codex is primarily written in Java but also uses Python for some tasks.

### Java

You need a Java JDK and [Maven](https://maven.apache.org/) to build and run Omega Codex.

### Python

You need [Python](https://www.python.org/) to run Omega Codex.

Before running Omega Codex you must create a Python virtual environment.
From the `python-tools` directory, run: `python -m venv .venv`

If you wish to run Python scripts manually you should activate the virtual environment first.
From the `python-tools` directory, run the activation command for your platform:

| Platform             | Activation Command              |
|----------------------|---------------------------------|
| Windows (CMD)        | `.venv\Scripts\activate.bat`    |
| Windows (PowerShell) | `.venv\Scripts\Activate.ps1`    |
| Windows (Git Bash)   | `source .venv/Scripts/activate` |
| macOS/Linux          | `source .venv/bin/activate`     |

> [!TIP]
> Activation is only necessary if you're running Python scripts manually.
> Omega Codex calls Python scripts using the correct environment automatically.

## Building and Running

To build Omega Codex: `mvn package`

To run the *Embedding* proof of concept: `mvn exec:java -P embed`

To run the *Hello World in Python* proof of concept: `mvn exec:java -P hello`

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
