const API_BASE = 'http://localhost:8080'

export async function login(email, password) {
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  const data = await res.json()
  if (!res.ok) {
    throw new Error(data.message || '이메일 또는 비밀번호가 올바르지 않습니다.')
  }
  return data
}

export async function signup({ username, email, password }) {
  const res = await fetch(`${API_BASE}/api/auth/signup`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, email, password }),
  })
  const data = await res.json()
  if (!res.ok) {
    throw new Error(data.message || '회원가입에 실패했습니다.')
  }
  return data
}
