import {IMember} from '@/models';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {AtomEffect, atom} from 'recoil';
import {API_KEY, JOABANK_BANKID} from '@env';

interface IMemberData {
  isLogin: boolean;
  member: IMember | null;
}

interface IBankData {
  bankId: string;
  bankName: string;
  apiKey: string;
}

const defaultMemberData: IMemberData = {
  isLogin: false,
  member: null,
};

const defaultBankData: IBankData = {
  bankId: JOABANK_BANKID ?? '',
  bankName: '조아은행',
  apiKey: API_KEY ?? '',
};

export const persistAtom =
  <T>(key: string): AtomEffect<T> =>
  ({setSelf, onSet, trigger}) => {
    const loadPersisted = async () => {
      const savedValue = await AsyncStorage.getItem(key);

      if (savedValue != null) {
        setSelf(JSON.parse(savedValue));
      }
    };

    // Asynchronously set the persisted data
    if (trigger === 'get') {
      loadPersisted();
    }

    // Subscribe to state changes and persist them to AsyncStorage
    onSet((newValue, _, isReset) => {
      isReset
        ? AsyncStorage.removeItem(key)
        : AsyncStorage.setItem(key, JSON.stringify(newValue));
    });
  };

export const memberDataAtom = atom<IMemberData>({
  key: 'memberData',
  default: defaultMemberData,
  effects: [persistAtom('memberData')],
});

export const bankDataAtom = atom<IBankData>({
  key: 'bankData',
  default: defaultBankData,
  effects: [persistAtom('bankData')],
});
