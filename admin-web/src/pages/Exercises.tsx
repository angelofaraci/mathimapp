import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch, getErrorMessage } from '../lib/api';

type ExerciseType = 'MULTIPLE_CHOICE' | 'INPUT_VALUE' | 'MULTI_SELECT';
type ChoiceOption = { id: string; text: string };
type MultipleChoicePayload = { type: 'multipleChoice'; options: ChoiceOption[]; correctOptionId?: string };
type InputValuePayload = { type: 'inputValue'; placeholder?: string; correctValue?: string };
type MultiSelectPayload = { type: 'multiSelect'; options: ChoiceOption[]; correctOptionIds?: string[] };
type ExercisePayload = MultipleChoicePayload | InputValuePayload | MultiSelectPayload;
type AdminLessonOption = { id: string; title: string };
type AdminExercise = { id: string; lessonId: string; title: string; type: ExerciseType; payload: ExercisePayload };
type AdminExerciseListResponse = { items: AdminExercise[] };
type BaseFormState = { lessonId: string; title: string };
type MultipleChoiceFormState = BaseFormState & { type: 'MULTIPLE_CHOICE'; options: ChoiceOption[]; correctOptionId: string };
type InputValueFormState = BaseFormState & { type: 'INPUT_VALUE'; placeholder: string; correctValue: string };
type MultiSelectFormState = BaseFormState & { type: 'MULTI_SELECT'; options: ChoiceOption[]; correctOptionIds: string[] };
type ExerciseFormState = MultipleChoiceFormState | InputValueFormState | MultiSelectFormState;
type ExerciseMutationPayload = { lessonId: string; title: string; type: ExerciseType; payload: ExercisePayload };

const exerciseTypes: ExerciseType[] = ['MULTIPLE_CHOICE', 'INPUT_VALUE', 'MULTI_SELECT'];
const createOption = (text = ''): ChoiceOption => ({ id: crypto.randomUUID(), text });
const typeLabel = (type: ExerciseType): string => ({
  MULTIPLE_CHOICE: 'Multiple choice',
  INPUT_VALUE: 'Input value',
  MULTI_SELECT: 'Multi select',
}[type]);

function ensureOptions(options: ChoiceOption[]): ChoiceOption[] {
  const next = options.map((option) => ({ ...option }));
  while (next.length < 2) next.push(createOption());
  return next;
}

function emptyForm(base: BaseFormState = { lessonId: '', title: '' }): ExerciseFormState {
  const options = ensureOptions([]);
  return { ...base, type: 'MULTIPLE_CHOICE', options, correctOptionId: options[0]?.id ?? '' };
}

function switchFormType(current: ExerciseFormState, nextType: ExerciseType): ExerciseFormState {
  if (current.type === nextType) return current;

  const base = { lessonId: current.lessonId, title: current.title };
  if (nextType === 'INPUT_VALUE') return { ...base, type: nextType, placeholder: '', correctValue: '' };

  const options = ensureOptions(current.type === 'INPUT_VALUE' ? [] : current.options);
  if (nextType === 'MULTIPLE_CHOICE') {
    const correctOptionId = current.type === 'MULTI_SELECT'
      ? current.correctOptionIds[0]
      : current.type === 'MULTIPLE_CHOICE'
        ? current.correctOptionId
        : options[0]?.id;
    return {
      ...base,
      type: nextType,
      options,
      correctOptionId: options.some((option) => option.id === correctOptionId) ? correctOptionId ?? '' : options[0]?.id ?? '',
    };
  }

  return {
    ...base,
    type: nextType,
    options,
    correctOptionIds: current.type === 'MULTIPLE_CHOICE'
      ? (current.correctOptionId ? [current.correctOptionId] : [])
      : current.type === 'MULTI_SELECT'
        ? current.correctOptionIds.filter((optionId: string) => options.some((option) => option.id === optionId))
        : [],
  };
}

function buildMutationPayload(form: ExerciseFormState): ExerciseMutationPayload {
  const lessonId = form.lessonId.trim();
  const title = form.title.trim();

  if (!lessonId) throw new Error('Select a lesson before saving the exercise.');
  if (!title) throw new Error('Enter a prompt before saving the exercise.');

  if (form.type === 'INPUT_VALUE') {
    const correctValue = form.correctValue.trim();
    if (!correctValue) throw new Error('Input value exercises require a correct value.');
    const placeholder = form.placeholder.trim();
    return { lessonId, title, type: form.type, payload: { type: 'inputValue', ...(placeholder ? { placeholder } : {}), correctValue } };
  }

  const options = form.options.map((option) => ({ ...option, text: option.text.trim() }));
  if (options.length < 2) throw new Error(`${typeLabel(form.type)} exercises require at least 2 options.`);
  if (options.some((option) => !option.text)) throw new Error(`Every ${typeLabel(form.type).toLowerCase()} option must have text.`);

  if (form.type === 'MULTIPLE_CHOICE') {
    if (!options.some((option) => option.id === form.correctOptionId)) throw new Error('Select a valid correct option for the multiple choice exercise.');
    return { lessonId, title, type: form.type, payload: { type: 'multipleChoice', options, correctOptionId: form.correctOptionId } };
  }

  const correctOptionIds = Array.from(new Set(form.correctOptionIds));
  if (correctOptionIds.length === 0) throw new Error('Select at least 1 correct option for the multi select exercise.');
  if (correctOptionIds.some((optionId) => !options.some((option) => option.id === optionId))) throw new Error('Multi select correct answers must reference existing options.');
  return { lessonId, title, type: form.type, payload: { type: 'multiSelect', options, correctOptionIds } };
}

function describeOptions(payload: ExercisePayload): string {
  if (payload.type === 'inputValue') return payload.placeholder?.trim() || '—';
  return payload.options.map((option) => option.text).join(', ') || '—';
}

function describeCorrectAnswer(payload: ExercisePayload): string {
  if (payload.type === 'inputValue') return payload.correctValue?.trim() || '—';
  if (payload.type === 'multipleChoice') return payload.options.find((option) => option.id === payload.correctOptionId)?.text ?? '—';
  const selected = new Set(payload.correctOptionIds ?? []);
  return payload.options.filter((option) => selected.has(option.id)).map((option) => option.text).join(', ') || '—';
}

function formFromExercise(exercise: AdminExercise): ExerciseFormState {
  const base = { lessonId: exercise.lessonId, title: exercise.title };

  if (exercise.payload.type === 'inputValue') {
    return { ...base, type: 'INPUT_VALUE', placeholder: exercise.payload.placeholder ?? '', correctValue: exercise.payload.correctValue ?? '' };
  }

  const options = ensureOptions(exercise.payload.options);
  if (exercise.payload.type === 'multipleChoice') {
    const payload = exercise.payload;
    const correctOptionId = options.some((option) => option.id === payload.correctOptionId)
      ? payload.correctOptionId ?? ''
      : options[0]?.id ?? '';
    return { ...base, type: 'MULTIPLE_CHOICE', options, correctOptionId };
  }

  return {
    ...base,
    type: 'MULTI_SELECT',
    options,
    correctOptionIds: (exercise.payload.correctOptionIds ?? []).filter((optionId) => options.some((option) => option.id === optionId)),
  };
}

async function fetchLessons(): Promise<AdminLessonOption[]> {
  return (await apiFetch<{ items: AdminLessonOption[] }>('/admin/lessons')).items;
}

async function fetchExercises(lessonId: string): Promise<AdminExercise[]> {
  const path = lessonId ? `/admin/exercises?lessonId=${encodeURIComponent(lessonId)}` : '/admin/exercises';
  return (await apiFetch<AdminExerciseListResponse>(path)).items;
}

async function createExercise(form: ExerciseFormState): Promise<void> {
  await apiFetch('/admin/exercises', { method: 'POST', body: JSON.stringify({ id: crypto.randomUUID(), ...buildMutationPayload(form) }) });
}

async function updateExercise(exerciseId: string, form: ExerciseFormState): Promise<void> {
  await apiFetch(`/admin/exercises/${exerciseId}`, { method: 'PUT', body: JSON.stringify(buildMutationPayload(form)) });
}

async function deleteExercise(exerciseId: string): Promise<void> {
  await apiFetch(`/admin/exercises/${exerciseId}`, { method: 'DELETE' });
}

type ChoiceEditorProps = {
  mode: 'single' | 'multiple';
  options: ChoiceOption[];
  selectedIds: string[];
  disabled: boolean;
  onTextChange: (optionId: string, text: string) => void;
  onToggle: (optionId: string, checked: boolean) => void;
  onAdd: () => void;
  onRemove: (optionId: string) => void;
};

function ChoiceEditor({ mode, options, selectedIds, disabled, onTextChange, onToggle, onAdd, onRemove }: ChoiceEditorProps) {
  return (
    <div>
      {options.map((option, index) => (
        <div key={option.id} style={{ display: 'grid', gap: '0.5rem', marginBottom: '1rem' }}>
          <label>
            Option {index + 1}
            <input type="text" value={option.text} onChange={(event) => onTextChange(option.id, event.target.value)} disabled={disabled} />
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <input
              type={mode === 'single' ? 'radio' : 'checkbox'}
              name={mode === 'single' ? 'multiple-choice-correct-option' : undefined}
              checked={selectedIds.includes(option.id)}
              onChange={(event) => onToggle(option.id, mode === 'single' ? true : event.target.checked)}
              disabled={disabled}
            />
            Correct answer
          </label>
          <button type="button" className="secondary-btn" onClick={() => onRemove(option.id)} disabled={disabled || options.length <= 2}>
            Remove option
          </button>
        </div>
      ))}
      <button type="button" className="secondary-btn" onClick={onAdd} disabled={disabled}>Add option</button>
    </div>
  );
}

export default function Exercises() {
  const queryClient = useQueryClient();
  const [filterLessonId, setFilterLessonId] = useState('');
  const [form, setForm] = useState<ExerciseFormState>(() => emptyForm());
  const [editingExerciseId, setEditingExerciseId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [errorBanner, setErrorBanner] = useState<string | null>(null);

  const lessonsQuery = useQuery({ queryKey: ['admin-lessons-options'], queryFn: fetchLessons });
  const exercisesQuery = useQuery({ queryKey: ['admin-exercises', filterLessonId], queryFn: () => fetchExercises(filterLessonId) });

  const createMutation = useMutation({
    mutationFn: createExercise,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin-exercises'] });
      setForm(emptyForm());
      setFeedback('Exercise created successfully.');
      setErrorBanner(null);
    },
    onError: (error) => setErrorBanner(getErrorMessage(error, 'Failed to create exercise.')),
  });

  const updateMutation = useMutation({
    mutationFn: ({ exerciseId, payload }: { exerciseId: string; payload: ExerciseFormState }) => updateExercise(exerciseId, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin-exercises'] });
      setForm(emptyForm());
      setEditingExerciseId(null);
      setFeedback('Exercise updated successfully.');
      setErrorBanner(null);
    },
    onError: (error) => setErrorBanner(getErrorMessage(error, 'Failed to update exercise.')),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteExercise,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin-exercises'] });
      setFeedback('Exercise deleted successfully.');
      setErrorBanner(null);
    },
    onError: (error) => setErrorBanner(getErrorMessage(error, 'Failed to delete exercise.')),
  });

  const lessonOptions = lessonsQuery.data ?? [];
  const exercises = exercisesQuery.data ?? [];
  const saving = createMutation.isPending || updateMutation.isPending;

  function resetForm() {
    setForm(emptyForm());
    setEditingExerciseId(null);
    setFeedback(null);
    setErrorBanner(null);
  }

  function updateOption(optionId: string, text: string) {
    setForm((current) => current.type === 'INPUT_VALUE' ? current : { ...current, options: current.options.map((option) => option.id === optionId ? { ...option, text } : option) });
  }

  function addOption() {
    setForm((current) => current.type === 'INPUT_VALUE' ? current : { ...current, options: [...current.options, createOption()] });
  }

  function removeOption(optionId: string) {
    setForm((current) => {
      if (current.type === 'INPUT_VALUE' || current.options.length <= 2) return current;
      const options = current.options.filter((option) => option.id !== optionId);
      return current.type === 'MULTIPLE_CHOICE'
        ? { ...current, options, correctOptionId: current.correctOptionId === optionId ? options[0]?.id ?? '' : current.correctOptionId }
        : { ...current, options, correctOptionIds: current.correctOptionIds.filter((currentOptionId) => currentOptionId !== optionId) };
    });
  }

  function toggleCorrectOption(optionId: string, checked: boolean) {
    setForm((current) => {
      if (current.type === 'MULTIPLE_CHOICE') return { ...current, correctOptionId: optionId };
      if (current.type !== 'MULTI_SELECT') return current;
      return { ...current, correctOptionIds: checked ? Array.from(new Set([...current.correctOptionIds, optionId])) : current.correctOptionIds.filter((currentOptionId) => currentOptionId !== optionId) };
    });
  }

  function lessonLabel(lessonId: string): string {
    return lessonOptions.find((lesson) => lesson.id === lessonId)?.title ?? lessonId;
  }

  function renderTypeFields() {
    if (form.type === 'INPUT_VALUE') {
      return (
        <>
          <label>
            Placeholder (optional)
            <input type="text" value={form.placeholder} onChange={(event) => setForm((current) => current.type === 'INPUT_VALUE' ? { ...current, placeholder: event.target.value } : current)} disabled={saving} />
          </label>
          <label>
            Correct value
            <input type="text" value={form.correctValue} onChange={(event) => setForm((current) => current.type === 'INPUT_VALUE' ? { ...current, correctValue: event.target.value } : current)} required disabled={saving} />
          </label>
        </>
      );
    }

    return (
      <>
        <p>{form.type === 'MULTIPLE_CHOICE' ? 'Options and exactly one correct answer.' : 'Options with one or more correct answers.'}</p>
        <ChoiceEditor
          mode={form.type === 'MULTIPLE_CHOICE' ? 'single' : 'multiple'}
          options={form.options}
          selectedIds={form.type === 'MULTIPLE_CHOICE' ? [form.correctOptionId] : form.correctOptionIds}
          disabled={saving}
          onTextChange={updateOption}
          onToggle={toggleCorrectOption}
          onAdd={addOption}
          onRemove={removeOption}
        />
      </>
    );
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFeedback(null);
    setErrorBanner(null);
    if (editingExerciseId) return updateMutation.mutate({ exerciseId: editingExerciseId, payload: form });
    createMutation.mutate(form);
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
              <select value={form.lessonId} onChange={(event) => setForm((current) => ({ ...current, lessonId: event.target.value }))} required disabled={saving || lessonsQuery.isLoading}>
                <option value="">Select a lesson</option>
                {lessonOptions.map((lesson) => <option key={lesson.id} value={lesson.id}>{lesson.title}</option>)}
              </select>
            </label>

            <label>
              Type
              <select value={form.type} onChange={(event) => setForm((current) => switchFormType(current, event.target.value as ExerciseType))} disabled={saving}>
                {exerciseTypes.map((type) => <option key={type} value={type}>{typeLabel(type)}</option>)}
              </select>
            </label>

            <label>
              Prompt
              <textarea value={form.title} onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))} rows={4} required disabled={saving} />
            </label>

            {renderTypeFields()}

            <div className="form-actions">
              <button type="submit" disabled={saving}>{editingExerciseId ? 'Save changes' : 'Create exercise'}</button>
              {editingExerciseId && <button type="button" className="secondary-btn" onClick={resetForm}>Cancel</button>}
            </div>
          </form>
        </section>

        <section className="panel">
          <div className="panel-header">
            <h3>Exercise list</h3>
            <label>
              Filter by lesson
              <select value={filterLessonId} onChange={(event) => setFilterLessonId(event.target.value)}>
                <option value="">All lessons</option>
                {lessonOptions.map((lesson) => <option key={lesson.id} value={lesson.id}>{lesson.title}</option>)}
              </select>
            </label>
          </div>

          {exercisesQuery.isLoading && <p>Loading exercises...</p>}
          {exercisesQuery.isError && <p className="error-text">Error: {(exercisesQuery.error as Error).message}</p>}

          {exercisesQuery.data && (
            <table className="data-table">
              <thead>
                <tr><th>ID</th><th>Prompt</th><th>Lesson</th><th>Type</th><th>Options / Placeholder</th><th>Correct answer</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {exercises.length === 0 ? (
                  <tr><td colSpan={7} className="empty-row">No exercises found.</td></tr>
                ) : exercises.map((exercise) => (
                  <tr key={exercise.id}>
                    <td className="id-cell">{exercise.id}</td>
                    <td className="desc-cell">{exercise.title}</td>
                    <td>{lessonLabel(exercise.lessonId)}</td>
                    <td>{typeLabel(exercise.type)}</td>
                    <td className="desc-cell">{describeOptions(exercise.payload)}</td>
                    <td>{describeCorrectAnswer(exercise.payload)}</td>
                    <td>
                      <div className="table-actions">
                        <button type="button" className="secondary-btn" onClick={() => { setForm(formFromExercise(exercise)); setEditingExerciseId(exercise.id); setFeedback(null); setErrorBanner(null); }}>Edit</button>
                        <button type="button" className="danger-btn" onClick={() => { if (window.confirm(`Delete exercise "${exercise.title}"?`)) { setFeedback(null); setErrorBanner(null); deleteMutation.mutate(exercise.id); } }} disabled={deleteMutation.isPending}>Delete</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      </div>
    </div>
  );
}
