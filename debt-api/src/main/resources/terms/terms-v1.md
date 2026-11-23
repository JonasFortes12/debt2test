# Debt2Test terms of use (v1)

Debt2Test is a research prototype built for a master's degree study. It analyses a Java repository you
point it at, finds self-admitted technical debt (SATD) comments, and generates candidate unit tests for them.
By asking for an access code you confirm that you have read and accept these terms.

## 1. Issue-tracker credentials

If you give a Jira email and API token for a run, they are used only to read issues during that run.
They are never stored, never written to logs, never returned by the API, and never shared with anyone.

## 2. Your source code and the LLM provider

To generate tests, Debt2Test sends parts of the analysed repository (SATD comments and the code around
them, plus related issue text) to the large language model (LLM) provider configured on this server.
That provider processes the data under its own API data-usage policy. Do not analyse code you are not
allowed to share with such a provider.

## 3. What is kept, and why

For the research study, Debt2Test keeps:

- your name and email address;
- your consent to these terms: the terms version, the time, and your IP address;
- the runs you start and their results (detected debt, classifications, generated tests, errors).

These records are kept for <retention period> and are used only for the study. They are not sold or
shared with third parties for any other purpose. Published results never identify you personally.

## 4. Deleting your data

To have your name, email and consent records deleted, write to <contact email>. Run results may be kept
in anonymised form after deletion, because they can no longer be linked to you.

## 5. Your responsibility

Only submit repositories you have the right to analyse. Each access code starts one run, and each email
address can start a limited number of runs.
