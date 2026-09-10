import { v4 as uuidv4 } from 'uuid';
import { User } from '../../types.js';

export interface IAuthService {
  getCurrentUser(): Promise<User | null>;
  login(email: string, password?: string): Promise<User>;
  register(email: string, password?: string): Promise<User>;
  logout(): Promise<void>;
  resetPassword(email: string): Promise<boolean>;
}

export class MockAuthService implements IAuthService {
  private currentUser: User | null = {
    id: 'user_dev_01',
    email: 'developer@phone-ai-ide.internal',
    createdAt: Date.now() - 86400000 * 7
  };

  async getCurrentUser(): Promise<User | null> {
    return this.currentUser;
  }

  async login(email: string): Promise<User> {
    this.currentUser = {
      id: 'user_' + Buffer.from(email).toString('hex').slice(0, 8),
      email,
      createdAt: Date.now()
    };
    return this.currentUser;
  }

  async register(email: string): Promise<User> {
    return this.login(email);
  }

  async logout(): Promise<void> {
    this.currentUser = null;
  }

  async resetPassword(email: string): Promise<boolean> {
    return email.includes('@');
  }
}

export const authService = new MockAuthService();
