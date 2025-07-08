# Omega Codex

*Omega Codex* is an AI-powered assistant that helps you explore, understand, and develop software projects.
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

## Building and Running

Omega Codex uses [Maven](https://maven.apache.org/) to manage builds.

To build Omega Codex: `mvn package`

To run Omega Codex: `mvn exec:java`
