import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch, getErrorMessage } from '../lib/api';

type ExerciseType = 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'INPUT_VALUE';

interface AdminLessonOption {
  id: string;
  title: string;
}

interface AdminExercise {
  id: string;
  lessonId: string;
  question: string;
  options: string[];
  correctAnswer: string;
  type: ExerciseType;
}

interface AdminExerciseListResponse {
  items: AdminExercise[];
}

interface ExerciseFormState {
  lessonId: string;
  question: string;
  optionsText: string;
  correctAnswer: string;
  type: ExerciseType;
}

const exerciseTypes: ExerciseType[] = [
  'MULTIPLE_CHOICE',
  'TRUE_FALSE',
  'INPUT_VALUE',
];

const emptyForm: ExerciseFormState = {
  lessonId: '',
  question: '',
  optionsText: '',
  correctAnswer: '',
  type: 'MULTIPLE_CHOICE',
};

async function fetchLessons(): Promise<AdminLessonOption[]> {
  const response = await apiFetch<{ items: AdminLessonOption[] }>('/admin/lessons');
  return response.items;
}

async function fetchExercises(lessonId: string): Promise<AdminExercise[]> {
  const path = lessonId ? `/admin/exercises?lessonId=${encodeURIComponent(lessonId)}` : '/admin/exercises';
  const response = await apiFetch<AdminExerciseListResponse>(path);
  return response.items;
}

function parseOptions(optionsText: string): string[] {
  return optionsText
    .split('\n')
    .map((option) => option.trim())
    .filter(Boolean);
}

async function createExercise(payload: ExerciseFormState): Promise<void> {
  await apiFetch('/admin/exercises', {
    method: 'POST',
    body: JSON.stringify({
      id: crypto.randomUUID(),
      lessonId: payload.lessonId,
      question: payload.question,
      options: parseOptions(payload.optionsText),
      correctAnswer: payload.correctAnswer,
      type: payload.type,
    }),
  });
}

async function updateExercise(
  exerciseId: string,
  payload: ExerciseFormState,
): Promise<void> {
  await apiFetch(`/admin/exercises/${exerciseId}`, {
    method: 'PUT',
    body: JSON.stringify({
      lessonId: payload.lessonId,
      question: payload.question,
      options: parseOptions(payload.optionsText),
      correctAnswer: payload.correctAnswer,
      type: payload.type,
    }),
  });
}

async function deleteExercise(exerciseId: string): Promise<void> {
  await apiFetch(`/admin/exercises/${exerciseId}`, {
    method: 'DELETE',
  });
}

export default function Exercises() {
  const queryClient = useQueryClient();
  const [filterLessonId, setFilterLessonId] = useState('');
  const [form, setForm] = useState<ExerciseFormState>(emptyForm);
  const [editingExerciseId, setEditingExerciseId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [errorBanner, setErrorBanner] = useState<string | null>(null);

  const lessonsQuery = useQuery({
    queryKey: ['admin-lessons-options'],
    queryFn: fetchLessons,
  });

  const exercisesQuery = useQuery({
    queryKey: ['admin-exercises', filterLessonId],
    queryFn: () => fetchExercises(filterLessonId),
  });

  const createMutation = useMutation({
    mutationFn: createExercise,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin-exercises'] });
      setForm(emptyForm);
      setFeedback('Exercise created successfully.');
      setErrorBanner(null);
    },
    onError: (mutationError) => {
      setErrorBanner(getErrorMessage(mutationError, 'Failed to create exercise.'));
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ exerciseId, payload }: { exerciseId: string; payload: ExerciseFormState }) =>
      updateExercise(exerciseId, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin-exercises'] });
      setForm(emptyForm);
      setEditingExerciseId(null);
      setFeedback('Exercise updated successfully.');
      setErrorBanner(null);
    },
    onError: (mutationError) => {
      setErrorBanner(getErrorMessage(mutationError, 'Failed to update exercise.'));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteExercise,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin-exercises'] });
      setFeedback('Exercise deleted successfully.');
      setErrorBanner(null);
    },
    onError: (mutationError) => {
      setErrorBanner(getErrorMessage(mutationError, 'Failed to delete exercise.'));
    },
  });

  const lessonOptions = lessonsQuery.data ?? [];
  const exercises = exercisesQuery.data ?? [];
  const isSaving = createMutation.isPending || updateMutation.isPending;

  function resetForm() {
    setForm(emptyForm);
    setEditingExerciseId(null);
    setErrorBanner(null);
  }

  function startEditing(exercise: AdminExercise) {
    setForm({
      lessonId: exercise.lessonId,
      question: exercise.question,
      optionsText: exercise.options.join('\n'),
      correctAnswer: exercise.correctAnswer,
      type: exercise.type,
    });
    setEditingExerciseId(exercise.id);
    setFeedback(null);
    setErrorBanner(null);
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFeedback(null);
    setErrorBanner(null);

    if (!form.lessonId) {
      setErrorBanner('Select a lesson before saving the exercise.');
      return;
    }

    if (editingExerciseId) {
      updateMutation.mutate({ exerciseId: editingExerciseId, payload: form });
      return;
    }

    createMutation.mutate(form);
  }

  function handleDelete(exercise: AdminExercise) {
    if (!window.confirm(`Delete exercise "${exercise.question}"?`)) {
      return;
    }

    setFeedback(null);
    setErrorBanner(null);
    deleteMutation.mutate(exercise.id);
  }

  function lessonLabel(lessonId: string): string {
    return lessonOptions.find((lesson) => lesson.id === lessonId)?.title ?? lessonId;
  }

  return (
    <div className="page">
      <h2>Exercises</h2>

      {feedback && <div className="success-banner">{feedback}</div>}
      {errorBanner && <div className="error-banner">{errorBanner}</div>}

      <div className="content-grid">
        <section className="panel">
          <h3>{editingExerciseId ? 'Edit exercise' : 'Create exercise'}</h3>
          <form className="entity-form" onSubmit={handleSubmit}>
            <label>
              Lesson
              <select
                value={form.lessonId}
                onChange={(event) =>
                  setForm((current) => ({ ...current, lessonId: event.target.value }))
                }
                required
                disabled={isSaving || lessonsQuery.isLoading}
              >
                <option value="">Select a lesson</option>
                {lessonOptions.map((lesson) => (
                  <option key={lesson.id} value={lesson.id}>
                    {lesson.title}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Type
              <select
                value={form.type}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    type: event.target.value as ExerciseType,
                  }))
                }
                disabled={isSaving}
              >
                {exerciseTypes.map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Question
              <textarea
                value={form.question}
                onChange={(event) =>
                  setForm((current) => ({ ...current, question: event.target.value }))
                }
                rows={4}
                required
                disabled={isSaving}
              />
            </label>

            <label>
              Options (one per line)
              <textarea
                value={form.optionsText}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    optionsText: event.target.value,
                  }))
                }
                rows={4}
                disabled={isSaving}
              />
            </label>

            <label>
              Correct answer
              <input
                type="text"
                value={form.correctAnswer}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    correctAnswer: event.target.value,
                  }))
                }
                required
                disabled={isSaving}
              />
            </label>

            <div className="form-actions">
              <button type="submit" disabled={isSaving}>
                {editingExerciseId ? 'Save changes' : 'Create exercise'}
              </button>
              {editingExerciseId && (
                <button type="button" className="secondary-btn" onClick={resetForm}>
                  Cancel
                </button>
              )}
            </div>
          </form>
        </section>

        <section className="panel">
          <div className="panel-header">
            <h3>Exercise list</h3>
            <label>
              Filter by lesson
              <select
                value={filterLessonId}
                onChange={(event) => setFilterLessonId(event.target.value)}
              >
                <option value="">All lessons</option>
                {lessonOptions.map((lesson) => (
                  <option key={lesson.id} value={lesson.id}>
                    {lesson.title}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {exercisesQuery.isLoading && <p>Loading exercises...</p>}
          {exercisesQuery.isError && (
            <p className="error-text">Error: {(exercisesQuery.error as Error).message}</p>
          )}

          {exercisesQuery.data && (
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Question</th>
                  <th>Lesson</th>
                  <th>Type</th>
                  <th>Options</th>
                  <th>Correct answer</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {exercises.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="empty-row">
                      No exercises found.
                    </td>
                  </tr>
                ) : (
                  exercises.map((exercise) => (
                    <tr key={exercise.id}>
                      <td className="id-cell">{exercise.id}</td>
                      <td className="desc-cell">{exercise.question}</td>
                      <td>{lessonLabel(exercise.lessonId)}</td>
                      <td>{exercise.type}</td>
                      <td className="desc-cell">{exercise.options.join(', ') || '—'}</td>
                      <td>{exercise.correctAnswer}</td>
                      <td>
                        <div className="table-actions">
                          <button type="button" className="secondary-btn" onClick={() => startEditing(exercise)}>
                            Edit
                          </button>
                          <button
                            type="button"
                            className="danger-btn"
                            onClick={() => handleDelete(exercise)}
                            disabled={deleteMutation.isPending}
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          )}
        </section>
      </div>
    </div>
  );
}
