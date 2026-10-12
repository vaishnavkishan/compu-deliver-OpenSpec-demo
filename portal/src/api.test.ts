import { createApi, ApiError } from './api'

afterEach(() => vi.unstubAllGlobals())

describe('api client', () => {
  it('sends role and client headers', async () => {
    const f = vi.fn().mockResolvedValue({ ok: true, json: async () => [] })
    vi.stubGlobal('fetch', f)
    await createApi({ role: 'ENTERPRISE_CLIENT', clientId: 3 }).orders()
    expect(f).toHaveBeenCalledWith('/api/orders', expect.objectContaining({
      headers: expect.objectContaining({ 'X-Role': 'ENTERPRISE_CLIENT', 'X-Client-Id': '3' }) }))
  })
  it('omits client id for operator and surfaces error message', async () => {
    const f = vi.fn().mockResolvedValue({ ok: false, status: 409, statusText: 'Conflict', json: async () => ({ message: 'boom' }) })
    vi.stubGlobal('fetch', f)
    const api = createApi({ role: 'WAREHOUSE_OPERATOR' })
    await expect(api.advance(1)).rejects.toMatchObject({ status: 409, message: 'boom' })
    expect(f.mock.calls[0][1].headers['X-Client-Id']).toBeUndefined()
    expect(new ApiError(1, 'x')).toBeInstanceOf(Error)
  })
})
