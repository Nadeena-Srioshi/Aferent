import * as yup from 'yup'

export const loginSchema = yup.object({
  email: yup.string().required('Email is required').email('Please enter a valid email'),
  password: yup.string().required('Password is required').min(6, 'Password must be at least 6 characters'),
})

export const adminSchema = yup.object({
  email: yup.string().required('Email is required').email('Please enter a valid email'),
  password: yup
    .string()
    .required('Password is required')
    .min(8, 'Password must be at least 8 characters')
    .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, 'Password must contain uppercase, lowercase and number'),
  confirmPassword: yup
    .string()
    .required('Please confirm your password')
    .oneOf([yup.ref('password')], 'Passwords must match'),
})

export const hospitalSchema = yup.object({
  name: yup.string().required('Hospital name is required').min(3, 'Name must be at least 3 characters'),
  address: yup.string().required('Address is required'),
  city: yup.string().required('City is required'),
  country: yup.string().required('Country is required'),
  phone: yup
    .string()
    .required('Phone number is required')
    .matches(/^[0-9+\-\s()]+$/, 'Please enter a valid phone number'),
  email: yup.string().required('Email is required').email('Please enter a valid email'),
  registrationNumber: yup.string().required('Registration number is required'),
  licenseInfo: yup.string().required('License information is required'),
})

export const specializationSchema = yup.object({
  name: yup.string().required('Specialization name is required').min(3, 'Name must be at least 3 characters'),
  description: yup.string().required('Description is required').min(10, 'Description must be at least 10 characters'),
  iconUrl: yup.string().url('Please enter a valid URL').nullable(),
})

export const doctorVerificationSchema = yup.object({
  status: yup.string().required('Status is required').oneOf(['APPROVED', 'REJECTED'], 'Invalid status'),
  comments: yup.string().when('status', {
    is: 'REJECTED',
    then: (schema) => schema.required('Rejection reason is required'),
    otherwise: (schema) => schema.nullable(),
  }),
})

export const refundSchema = yup.object({
  reason: yup.string().required('Refund reason is required').min(10, 'Reason must be at least 10 characters'),
  amount: yup.number().required('Refund amount is required').positive('Amount must be positive'),
})

export const faqSchema = yup.object({
  question: yup.string().required('Question is required').min(10, 'Question must be at least 10 characters'),
  answer: yup.string().required('Answer is required').min(20, 'Answer must be at least 20 characters'),
  category: yup.string().required('Category is required'),
})

export const contentSchema = yup.object({
  title: yup.string().required('Title is required'),
  content: yup.string().required('Content is required').min(50, 'Content must be at least 50 characters'),
  version: yup.string().required('Version is required'),
})
