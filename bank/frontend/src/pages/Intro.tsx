import {
  Alert,
  Image,
  ScrollView,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import {useEffect, useState} from 'react';
import {NativeStackScreenProps} from '@react-navigation/native-stack';
import {Controller, useForm} from 'react-hook-form';
import Header from '@/components/Header';
import PiggyBank2 from '@/assets/piggy-bank2.png';
import BottomPopup from '@/components/BottomPopup';
import CommonInput from '@/components/CommonInput';
import BottomButton from '@/components/BottomButton';
import {useFocusEffect} from '@react-navigation/native';
import {useCallback} from 'react';
import {useRecoilValue, useSetRecoilState} from 'recoil';
import {bankDataAtom, memberDataAtom} from '@/store/atoms';
import {RootStackParamList} from '@/Router';
import LoadingScreen from '@/components/LoadingScreen';
import {useMutation} from '@tanstack/react-query';
import {login} from '@/api/member';
import {axiosInstance} from '@/api';
import {IMember} from '@/models';

interface LoginForm {
  email: string;
  password: string;
}

type IntroScreenProps = NativeStackScreenProps<RootStackParamList, 'Intro'>;

function Intro({navigation}: IntroScreenProps): React.JSX.Element {
  const setMemberData = useSetRecoilState(memberDataAtom);
  const bankData = useRecoilValue(bankDataAtom);
  const mutation = useMutation({
    mutationFn: login,
    onSuccess: data => {
      console.log(data);
      const member: IMember = data.data;
      Alert.alert(`환영합니다. ${member.name}님`);
      axiosInstance.interceptors.request.clear();
      axiosInstance.interceptors.request.use(
        config => {
          config.headers.memberId = member.id;
          config.headers.apiKey = bankData.apiKey;
          return config;
        },
        error => {
          return Promise.reject(error);
        },
      );
      setMemberData({
        isLogin: true,
        member: data.data,
      });
    },
    onError: err => console.log(err),
  });
  const {
    control,
    handleSubmit,
    formState: {errors},
    reset,
  } = useForm<LoginForm>({
    defaultValues: {
      email: '',
      password: '',
    },
  });
  const [loginModalOpen, setLoginModalOpen] = useState<boolean>(false);
  const [showPassword, setShowPassword] = useState<boolean>(false);
  const onSubmit = (data: LoginForm) => {
    mutation.mutate({
      email: data.email,
      password: data.password,
      bankId: bankData.bankId,
    });
  };

  useFocusEffect(
    useCallback(() => {
      setLoginModalOpen(false);
    }, []),
  );

  useEffect(() => {
    reset();
  }, [loginModalOpen, reset]);

  return (
    <View className="w-full h-full bg-gray-100">
      <Header
        menu={[
          {title: 'magnify', onPress: () => navigation.navigate('Search')},
          {title: 'menu', onPress: () => navigation.navigate('Menu')},
        ]}
      />
      <View className="w-full h-80 flex-grow bg-gray-100 pt-36 px-6 pb-24 flex justify-between">
        <View className="w-full flex px-4 space-y-2">
          <Text className="w-full font-semibold text-xl text-gray-700">
            {bankData.bankName}에
          </Text>
          <Text className="w-full font-semibold text-xl text-gray-700">
            오신 것을 환영합니다.
          </Text>
        </View>
        <View className="w-full flex space-y-4 relative">
          <Image
            source={PiggyBank2}
            className="w-36 h-36 absolute -top-24 -right-4 z-10"
          />
          <TouchableOpacity
            onPress={() => navigation.push('Join')}
            className="w-full h-24 bg-pink-200 shadow-sm shadow-black flex flex-row space-x-8 items-center px-8">
            <Icon
              name={'account-outline'}
              color={'#777'}
              onPress={() => {}}
              size={30}
            />
            <View className="flex space-y-2">
              <Text className="font-bold text-lg text-gray-700">회원가입</Text>
              <Text className="font-semibold text-xs text-gray-700">
                {bankData.bankName}이 처음이신가요?
              </Text>
            </View>
          </TouchableOpacity>
          <TouchableOpacity
            onPress={() => {
              setLoginModalOpen(true);
            }}
            className="w-full h-24 bg-gray-200 shadow-sm shadow-black flex flex-row space-x-8 items-center px-8">
            <Icon name={'login'} color={'#777'} onPress={() => {}} size={30} />
            <View className="flex space-y-2">
              <Text className="font-bold text-lg text-gray-700">로그인</Text>
              <Text className="font-semibold text-xs text-gray-700">
                이미 {bankData.bankName}을 사용하고 계신가요?
              </Text>
            </View>
          </TouchableOpacity>
        </View>
      </View>
      {loginModalOpen && (
        <BottomPopup close={() => setLoginModalOpen(false)}>
          <ScrollView className="w-full flex flex-grow">
            <View className="w-full space-y-6 mb-17 py-2 ">
              <CommonInput label={'이메일'}>
                <Controller
                  control={control}
                  rules={{
                    required: '이메일을 입력해주세요.',
                    pattern: {
                      value:
                        /^[0-9a-zA-Z]([-_\.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_\.]?[0-9a-zA-Z])*\.[a-zA-Z]{2,3}$/,
                      message: '올바른 이메일 형식이 아닙니다.',
                    },
                  }}
                  render={({field: {onChange, onBlur, value}}) => (
                    <TextInput
                      className="border-b border-gray-800/50 text-gray-700"
                      onBlur={onBlur}
                      onChangeText={onChange}
                      value={value}
                      keyboardType="email-address"
                      autoCapitalize="none"
                      autoCorrect={false}
                      autoComplete="email"
                    />
                  )}
                  name="email"
                />
                <Text className="absolute bottom-2 left-8 text-red-400">
                  {errors.email?.message}
                </Text>
              </CommonInput>
              <CommonInput label={'비밀번호'}>
                <Controller
                  control={control}
                  rules={{required: '비밀번호를 입력해주세요.'}}
                  render={({field: {onChange, onBlur, value}}) => (
                    <View className="w-full relative">
                      <TextInput
                        className="border-b border-gray-800/50 text-gray-700"
                        onBlur={onBlur}
                        onChangeText={onChange}
                        value={value}
                        secureTextEntry={!showPassword}
                        autoCapitalize="none"
                      />
                      <TouchableOpacity className="absolute right-0 top-0 translate-y-3 p-2">
                        <Icon
                          name={
                            showPassword ? 'eye-outline' : 'eye-off-outline'
                          }
                          color={'#777'}
                          onPress={() => setShowPassword(!showPassword)}
                          size={20}
                        />
                      </TouchableOpacity>
                    </View>
                  )}
                  name="password"
                />
                <Text className="absolute bottom-2 left-8 text-red-400">
                  {errors.password?.message}
                </Text>
              </CommonInput>
              <View className="w-full flex flex-row justify-center space-x-8">
                <TouchableOpacity onPress={() => navigation.navigate('Join')}>
                  <Text className="text-gray-700">회원가입</Text>
                </TouchableOpacity>
                <Text className="text-gray-700">|</Text>
                <TouchableOpacity>
                  <Text className="text-gray-700">비밀번호 찾기</Text>
                </TouchableOpacity>
              </View>
            </View>
          </ScrollView>
          <BottomButton title={'로그인'} onPress={handleSubmit(onSubmit)} />
        </BottomPopup>
      )}
      <LoadingScreen isLoading={false} />
    </View>
  );
}

export default Intro;
