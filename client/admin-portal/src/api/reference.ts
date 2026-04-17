import api from './axios'

export interface Hospital {
  id: string
  name: string
  createdAt?: string
  updatedAt?: string
}

export interface Specialization {
  id: string
  name: string
  createdAt?: string
  updatedAt?: string
}

export interface ReferenceDataRequest {
  name: string
}

export const referenceApi = {
  // Hospitals
  getAllHospitals: () => api.get<Hospital[]>('/hospitals'),
  addHospital: (data: ReferenceDataRequest) => api.post<Hospital>('/admin/hospitals', data),
  updateHospital: (id: string, data: ReferenceDataRequest) => api.put<Hospital>(`/admin/hospitals/${id}`, data),
  deleteHospital: (id: string) => api.delete<void>(`/admin/hospitals/${id}`),

  // Specializations
  getAllSpecializations: () => api.get<Specialization[]>('/specializations'),
  addSpecialization: (data: ReferenceDataRequest) => api.post<Specialization>('/admin/specializations', data),
  updateSpecialization: (id: string, data: ReferenceDataRequest) => api.put<Specialization>(`/admin/specializations/${id}`, data),
  deleteSpecialization: (id: string) => api.delete<void>(`/admin/specializations/${id}`),
}
