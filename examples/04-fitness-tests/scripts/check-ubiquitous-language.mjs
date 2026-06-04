#!/usr/bin/env node
/**
 * Ubiquitous-language gate.
 *
 * One concept, one word — across backend and frontend. A domain glossary
 * picks the blessed term for each idea ("Booking", not "Appointment" /
 * "Reservation" / "Session"; "Booker", not "User" / "Customer" / "EndUser").
 * This script greps the source tree for the banned synonyms and exits
 * non-zero if any slip in, so the build fails before the vocabulary drifts.
 *
 * Why bother? Synonym drift is how a codebase ends up with `Appointment`,
 * `Booking`, and `Session` all meaning the same thing — and how the next
 * person (or model) guesses wrong about which one to use. It's death by a
 * thousand near-synonyms. A model is especially prone to it: ask for a
 * "reservation feature" and you'll get a `Reservation` type next to your
 * `Booking` one unless something says no. This is that something.
 *
 * Run: `node scripts/check-ubiquitous-language.mjs`
 * Wire it into CI next to the Java fitness tests.
 */

import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, extname } from 'node:path';

/** banned term (case-insensitive, whole word) → the word to use instead */
const GLOSSARY = {
  Appointment: 'Booking',
  Reservation: 'Booking',
  EndUser: 'Booker',
  Customer: 'Booker',
  Venue: 'Branch',
  Timeslot: 'Slot',
  TimeBox: 'Slot',
};

/** Directories to scan for source. */
const SOURCE_ROOTS = ['examples', 'src', 'web'];

/** File extensions that count as source. */
const SOURCE_EXT = new Set(['.java', '.ts', '.tsx', '.mjs', '.js']);

/** Lines containing this marker are exempt (for the rare justified use). */
const ALLOW_MARKER = 'ubiquitous-language-allow';

/** Don't scan ourselves — this file necessarily names the banned words. */
const SELF = 'check-ubiquitous-language.mjs';

function* walk(dir) {
  let entries;
  try {
    entries = readdirSync(dir);
  } catch {
    return; // a configured root that doesn't exist is fine
  }
  for (const entry of entries) {
    if (entry === 'node_modules' || entry === 'build' || entry === 'dist' || entry.startsWith('.')) {
      continue;
    }
    const path = join(dir, entry);
    if (statSync(path).isDirectory()) {
      yield* walk(path);
    } else if (SOURCE_EXT.has(extname(path)) && !path.endsWith(SELF)) {
      yield path;
    }
  }
}

const patterns = Object.entries(GLOSSARY).map(([banned, preferred]) => ({
  banned,
  preferred,
  re: new RegExp(`\\b${banned}\\b`, 'i'),
}));

const violations = [];

for (const root of SOURCE_ROOTS) {
  for (const file of walk(root)) {
    const lines = readFileSync(file, 'utf8').split('\n');
    lines.forEach((line, i) => {
      if (line.includes(ALLOW_MARKER)) return;
      for (const { banned, preferred, re } of patterns) {
        if (re.test(line)) {
          violations.push({ file, line: i + 1, banned, preferred, text: line.trim() });
        }
      }
    });
  }
}

if (violations.length > 0) {
  console.error(`\n✗ Ubiquitous-language gate failed — ${violations.length} banned term(s):\n`);
  for (const v of violations) {
    console.error(`  ${v.file}:${v.line}  "${v.banned}" → use "${v.preferred}"`);
    console.error(`     ${v.text}`);
  }
  console.error(`\n  Fix the term, or append "// ${ALLOW_MARKER}: <reason>" if genuinely justified.\n`);
  process.exit(1);
}

console.log('✓ Ubiquitous-language gate passed — vocabulary is consistent.');
