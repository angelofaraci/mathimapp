import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiFetch, ApiError } from '../lib/api';

// ---- Types (mirror AdminDtos.kt) ----

interface AdminUser {
  id: string;
  name: string;
  email: string;
  role: string; // "ADMIN" | "TEACHER" | "STUDENT"
}

interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

const ROLES = ['STUDENT', 'TEACHER', 'ADMIN'] as const;

// ---- API calls ----

async function fetchUsers(
  page: number,
  size: number,
  query: string,
): Promise<PageResponse<AdminUser>> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  if (query.trim()) {
    params.set('query', query.trim());
  }
  return apiFetch<PageResponse<AdminUser>>(`/admin/users?${params}`);
}

async function updateRole(
  userId: string,
  role: string,
): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/admin/users/${userId}/role`, {
    method: 'PUT',
    body: JSON.stringify({ role }),
  });
}

// ---- Component ----

export default function Users() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [editingUserId, setEditingUserId] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Debounce search input: useEffect cleanup clears the previous timeout
  // on each keystroke, so only the last one fires after 400ms.
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(0);
    }, 400);
    return () => clearTimeout(timeoutId);
  }, [search]);

  const handleSearchChange = (value: string) => {
    setSearch(value);
    setSaveError(null);
  };

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['admin-users', page, debouncedSearch],
    queryFn: () => fetchUsers(page, 20, debouncedSearch),
  });

  const roleMutation = useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: string }) =>
      updateRole(userId, role),
    onSuccess: (updatedUser) => {
      // Optimistic update: replace user in cache
      queryClient.setQueryData<PageResponse<AdminUser>>(
        ['admin-users', page, debouncedSearch],
        (old) => {
          if (!old) return old;
          return {
            ...old,
            items: old.items.map((u) =>
              u.id === updatedUser.id ? updatedUser : u,
            ),
          };
        },
      );
      setEditingUserId(null);
      setSaveError(null);
      setSuccessMsg(`Role updated for ${updatedUser.name}`);
      setTimeout(() => setSuccessMsg(null), 3000);
    },
    onError: (err) => {
      if (err instanceof ApiError) {
        if (err.status === 404) {
          setSaveError('User no longer exists. Refresh the list.');
        } else if (err.status === 400) {
          setSaveError('Invalid role value.');
        } else {
          setSaveError(err.message);
        }
      } else {
        setSaveError('Failed to update role.');
      }
      setEditingUserId(null);
    },
  });

  function handleRoleChange(userId: string, newRole: string) {
    setEditingUserId(userId);
    setSaveError(null);
    setSuccessMsg(null);
    roleMutation.mutate({ userId, role: newRole });
  }

  return (
    <div className="page">
      <h2>Users</h2>

      {successMsg && <div className="success-banner">{successMsg}</div>}
      {saveError && <div className="error-banner">{saveError}</div>}

      <div className="search-bar">
        <input
          type="text"
          placeholder="Search by name or email..."
          value={search}
          onChange={(e) => handleSearchChange(e.target.value)}
        />
      </div>

      {isLoading && <p>Loading users...</p>}
      {isError && <p className="error-text">Error: {(error as Error).message}</p>}

      {data && (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.items.length === 0 ? (
                <tr>
                  <td colSpan={5} className="empty-row">
                    No users found.
                  </td>
                </tr>
              ) : (
                data.items.map((user) => (
                  <tr key={user.id}>
                    <td className="id-cell">{user.id}</td>
                    <td>{user.name}</td>
                    <td>{user.email}</td>
                    <td>
                      <select
                        value={user.role}
                        onChange={(e) =>
                          handleRoleChange(user.id, e.target.value)
                        }
                        disabled={
                          roleMutation.isPending && editingUserId === user.id
                        }
                      >
                        {ROLES.map((r) => (
                          <option key={r} value={r}>
                            {r}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      {roleMutation.isPending && editingUserId === user.id ? (
                        <span className="saving-label">Saving...</span>
                      ) : (
                        <span className="saved-label">✓</span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>

          <div className="pagination">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
            >
              Previous
            </button>
            <span>
              Page {page + 1} of {data.totalPages || 1}
              {' '}({data.totalElements} users)
            </span>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={page + 1 >= data.totalPages}
            >
              Next
            </button>
          </div>
        </>
      )}
    </div>
  );
}
