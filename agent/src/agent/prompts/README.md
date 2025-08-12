# Agent Prompts

This directory contains all the prompts used by the agent system. Each prompt is stored in a separate text file for easy maintenance and modification.

## Prompt Files

### `main_system_prompt.txt`
The main system prompt that defines the agent's core behavior and guidelines. This is loaded at the beginning of each conversation to establish the agent's role and constraints.

### `human_confirmation_prompt.txt`
Used by the HumanConfirmationNode to generate user-friendly descriptions of tool actions before requesting approval. This prompt helps convert technical tool calls into natural language that users can easily understand.

### `progress_report_prompt.txt`
Used by the ProgressReportNode to generate concise progress updates while tools are executing. This prompt ensures consistent formatting of progress messages with "..." to indicate ongoing operations.

## Usage

Prompts are loaded using the `prompt_loader` utility with configuration keys:

```python
from src.agent.utils.prompt_loader import load_prompt

# Load prompts using configuration keys
main_prompt = load_prompt("main_system")
confirmation_prompt = load_prompt("human_confirmation")
progress_prompt = load_prompt("progress_report")
```

## Configuration

All prompt paths are configured in `config.yaml`:

```yaml
agent:
  prompts_dir: "src/agent/prompts"
  prompts:
    main_system: "main_system_prompt.txt"
    human_confirmation: "human_confirmation_prompt.txt"
    progress_report: "progress_report_prompt.txt"
```

This approach follows industry standards by:
- Keeping all configuration in YAML files
- Using semantic keys instead of hardcoded filenames
- Allowing easy reconfiguration without code changes
- Supporting different environments with different prompt files
- Working correctly from Docker working directory (`WORKDIR agent`)

## Best Practices

1. Keep prompts focused and specific to their purpose
2. Use clear, concise language
3. Test prompt changes thoroughly
4. Document any special formatting requirements
5. Consider prompt length for token efficiency