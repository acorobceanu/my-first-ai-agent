export type SessionStatus = 'IN_PROGRESS' | 'COMPLETED';

export interface AnsweredQuestion {
  question: string;
  answer: string;
}

export interface ProfessionRecommendation {
  profession: string;
  confidence: number;
  reasons: string[];
  strengths: string[];
  growthAreas: string[];
  nextSteps: string[];
}

export interface AptitudeFindings {
  summary: string;
  recommendations: ProfessionRecommendation[];
  crossCuttingStrengths: string[];
  cautions: string[];
}

export interface SessionResponse {
  sessionId: string;
  status: SessionStatus;
  questionCount: number;
  maxQuestions: number;
  currentQuestion: string | null;
  answers: AnsweredQuestion[];
  findings: AptitudeFindings | null;
  createdAt: string;
  updatedAt: string;
}
