export type SessionStatus = 'IN_PROGRESS' | 'COMPLETED';
export type QuestionType = 'FREE_TEXT' | 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE';
export type QuestionOptionType = 'STANDARD' | 'ALL_OF_THE_ABOVE' | 'NONE_OF_THE_ABOVE';

export interface QuestionOption {
  id: string;
  label: string;
  type: QuestionOptionType;
}

export interface Question {
  type: QuestionType;
  prompt: string;
  options: QuestionOption[];
}

export interface AnsweredQuestion {
  question: string;
  answer: string;
  questionDetails?: Question | null;
  selectedOptionIds?: string[];
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
  question: Question | null;
  currentQuestion: string | null;
  answers: AnsweredQuestion[];
  findings: AptitudeFindings | null;
  createdAt: string;
  updatedAt: string;
}
