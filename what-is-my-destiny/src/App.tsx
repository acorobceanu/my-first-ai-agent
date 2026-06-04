import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import HistoryIcon from '@mui/icons-material/History';
import RestartAltIcon from '@mui/icons-material/RestartAlt';
import SendIcon from '@mui/icons-material/Send';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Container,
  Divider,
  LinearProgress,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { FormEvent, useMemo, useState } from 'react';
import { createSession, submitAnswer } from './api/aptitudeClient';
import type { AptitudeFindings, ProfessionRecommendation, SessionResponse } from './api/types';

const MAX_ANSWER_LENGTH = 2000;

export default function App() {
  const [session, setSession] = useState<SessionResponse | null>(null);
  const [answer, setAnswer] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isStarting, setIsStarting] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const progress = useMemo(() => {
    if (!session) {
      return 0;
    }

    return Math.min((session.questionCount / session.maxQuestions) * 100, 100);
  }, [session]);

  const startSession = async () => {
    setIsStarting(true);
    setError(null);

    try {
      const nextSession = await createSession();
      setSession(nextSession);
      setAnswer('');
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setIsStarting(false);
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!session || !answer.trim()) {
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      const nextSession = await submitAnswer(session.sessionId, answer.trim());
      setSession(nextSession);
      setAnswer('');
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Box className="app-shell">
      <Container maxWidth="lg" className="app-container">
        <Box className="masthead">
          <Stack spacing={2}>
            <Chip
              icon={<AutoAwesomeIcon />}
              label="What Is My Destiny?"
              color="primary"
              variant="outlined"
              className="brand-chip"
            />
            <Typography variant="h1">Find the work that fits the shape of you.</Typography>
            <Typography variant="body1" color="text.secondary" className="intro-copy">
              Answer a few focused questions from the aptitude agent. The backend keeps the
              session and returns the next question until it has enough evidence for a final recap.
            </Typography>
          </Stack>
        </Box>

        {error && (
          <Alert severity="error" className="status-alert">
            {error}
          </Alert>
        )}

        {!session && (
          <Card className="flow-card">
            <CardContent className="flow-card-content">
              <Stack spacing={3}>
                <Typography variant="h2">Begin your session</Typography>
                <Typography color="text.secondary">
                  The first click creates a backend session id. Every answer after that is posted
                  with the same id so the agent can keep context.
                </Typography>
                <Button
                  size="large"
                  variant="contained"
                  endIcon={<ArrowForwardIcon />}
                  onClick={startSession}
                  disabled={isStarting}
                  className="primary-action"
                >
                  {isStarting ? 'Starting...' : 'Start'}
                </Button>
              </Stack>
            </CardContent>
          </Card>
        )}

        {session?.status === 'IN_PROGRESS' && (
          <Stack spacing={3}>
            <Card className="flow-card">
              <CardContent className="flow-card-content">
                <Stack spacing={3}>
                  <SessionProgress session={session} progress={progress} />

                  <Divider />

                  <Stack spacing={1.5}>
                    <Typography variant="overline" color="text.secondary">
                      Current question
                    </Typography>
                    <Typography variant="h2">{session.currentQuestion}</Typography>
                  </Stack>

                  <Box component="form" onSubmit={handleSubmit}>
                    <Stack spacing={2}>
                      <TextField
                        label="Your answer"
                        value={answer}
                        onChange={(event) => setAnswer(event.target.value)}
                        multiline
                        minRows={6}
                        inputProps={{ maxLength: MAX_ANSWER_LENGTH }}
                        helperText={`${answer.length}/${MAX_ANSWER_LENGTH}`}
                        disabled={isSubmitting}
                        fullWidth
                      />
                      <Button
                        type="submit"
                        size="large"
                        variant="contained"
                        endIcon={<SendIcon />}
                        disabled={!answer.trim() || isSubmitting}
                        className="primary-action"
                      >
                        {isSubmitting ? 'Sending...' : 'Next'}
                      </Button>
                    </Stack>
                  </Box>
                </Stack>
              </CardContent>
            </Card>

            <AnswerHistory session={session} />
          </Stack>
        )}

        {session?.status === 'COMPLETED' && (
          <Stack spacing={3}>
            <CompletedFindings findings={session.findings} />
            <AnswerHistory session={session} expanded />
            <Button
              variant="outlined"
              startIcon={<RestartAltIcon />}
              onClick={startSession}
              disabled={isStarting}
              className="restart-action"
            >
              Start a new session
            </Button>
          </Stack>
        )}
      </Container>
    </Box>
  );
}

function SessionProgress({ session, progress }: { session: SessionResponse; progress: number }) {
  return (
    <Stack spacing={1.5}>
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={1}>
        <Stack spacing={0.5}>
          <Typography variant="overline" color="text.secondary">
            Session
          </Typography>
          <Typography className="session-id">{session.sessionId}</Typography>
        </Stack>
        <Chip
          label={`${session.questionCount} of ${session.maxQuestions}`}
          color="secondary"
          variant="outlined"
          className="progress-chip"
        />
      </Stack>
      <LinearProgress variant="determinate" value={progress} className="progress-bar" />
    </Stack>
  );
}

function AnswerHistory({ session, expanded = false }: { session: SessionResponse; expanded?: boolean }) {
  if (!session.answers.length) {
    return null;
  }

  return (
    <Card className="history-card">
      <CardContent>
        <Stack spacing={2}>
          <Stack direction="row" alignItems="center" spacing={1}>
            <HistoryIcon color="primary" />
            <Typography variant="h3">Recap</Typography>
          </Stack>
          <Stack spacing={2}>
            {session.answers.map((item, index) => (
              <Box key={`${item.question}-${index}`} className="answer-pair">
                <Typography variant="subtitle2" color="text.secondary">
                  Question {index + 1}
                </Typography>
                <Typography fontWeight={700}>{item.question}</Typography>
                {(expanded || index === session.answers.length - 1) && (
                  <Typography color="text.secondary">{item.answer}</Typography>
                )}
              </Box>
            ))}
          </Stack>
        </Stack>
      </CardContent>
    </Card>
  );
}

function CompletedFindings({ findings }: { findings: AptitudeFindings | null }) {
  if (!findings) {
    return (
      <Alert severity="info">
        The session is complete, but the backend did not include final findings.
      </Alert>
    );
  }

  return (
    <Card className="flow-card">
      <CardContent className="flow-card-content">
        <Stack spacing={3}>
          <Stack spacing={1.5}>
            <Chip
              icon={<CheckCircleIcon />}
              label="Final answer"
              color="primary"
              className="complete-chip"
            />
            <Typography variant="h2">Your destiny report is ready.</Typography>
            <Typography color="text.secondary">{findings.summary}</Typography>
          </Stack>

          <Stack className="recommendation-grid">
            {findings.recommendations.map((recommendation) => (
              <RecommendationCard key={recommendation.profession} recommendation={recommendation} />
            ))}
          </Stack>

          <SummaryLists findings={findings} />
        </Stack>
      </CardContent>
    </Card>
  );
}

function RecommendationCard({ recommendation }: { recommendation: ProfessionRecommendation }) {
  return (
    <Card className="recommendation-card">
      <CardContent>
        <Stack spacing={2}>
          <Stack direction="row" justifyContent="space-between" gap={2} alignItems="flex-start">
            <Typography variant="h3">{recommendation.profession}</Typography>
            <Chip label={`${recommendation.confidence}%`} color="secondary" />
          </Stack>
          <BulletList title="Why it fits" items={recommendation.reasons} />
          <BulletList title="Strengths" items={recommendation.strengths} />
          <BulletList title="Growth areas" items={recommendation.growthAreas} />
          <BulletList title="Next steps" items={recommendation.nextSteps} />
        </Stack>
      </CardContent>
    </Card>
  );
}

function SummaryLists({ findings }: { findings: AptitudeFindings }) {
  return (
    <Stack className="summary-grid">
      <BulletList title="Cross-cutting strengths" items={findings.crossCuttingStrengths} />
      <BulletList title="Cautions" items={findings.cautions} />
    </Stack>
  );
}

function BulletList({ title, items }: { title: string; items: string[] }) {
  if (!items.length) {
    return null;
  }

  return (
    <Box>
      <Typography variant="subtitle2" color="text.secondary" gutterBottom>
        {title}
      </Typography>
      <List dense disablePadding>
        {items.map((item) => (
          <ListItem key={item} disableGutters alignItems="flex-start">
            <ListItemIcon className="list-icon">
              <CheckCircleIcon color="primary" fontSize="small" />
            </ListItemIcon>
            <ListItemText primary={item} />
          </ListItem>
        ))}
      </List>
    </Box>
  );
}

function getErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Something went wrong.';
}
