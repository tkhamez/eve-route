import React, { forwardRef, useCallback, useContext, useImperativeHandle, useRef } from 'react';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Box,
  CircularProgress,
  IconButton,
  InputAdornment,
  List,
  ListItem,
  TextField,
} from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import ClearIcon from '@material-ui/icons/Clear';
import LocationOnOutlinedIcon from '@material-ui/icons/LocationOnOutlined';
import axios from 'axios';
import { GlobalDataContext } from '../GlobalDataContext';
import { ResponseRouteLocation, ResponseSystemNames } from '../response';

const useStyles = makeStyles((theme) => ({
  wrap: {
    position: 'relative',
    maxWidth: '300px',
  },
  textField: {
    '&:hover $clearIndicator, & .Mui-focused $clearIndicator': {
      visibility: 'visible'
    },
  },
  location: {
    color: theme.palette.grey[500],
  },
  clearIndicator: {
    visibility: 'hidden',
  },
  loadingHidden: {
    visibility: 'hidden',
  },
  box: {
    position: 'absolute',
    zIndex: 100,
    width: '100%',
    marginTop: '3px',
    backgroundColor: theme.palette.background.paper,
    borderRadius: '4px',
    maxHeight: '250px',
    overflowY: 'auto',
  },
}));

type Props = {
  fieldId: string,
  fieldName: string,
  onChange: Function,
  findRoute?: Function,
}

export default forwardRef((props: Props, ref: any) => {
  const { t } = useTranslation();
  const globalData = useContext(GlobalDataContext);
  const classes = useStyles();
  const [searchTerm, setSearchTerm] = useState('');
  const [systems, setSystems] = useState<string[]>([]);
  const [open, setOpen] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const startSystemId = "start-system";

  // copy props for use in useEffect()
  const propsOnChange = props.onChange;

  useImperativeHandle(ref, () => ({
    clearInput() {
      onChange('');
    }
  }));

  const fetchLocation = useCallback(() => {
    axios.get<ResponseRouteLocation>(`${globalData.domain}/api/route/location`).then(r => {
      if (r.data.code) {
        console.log(r.data.code);
      } else if (r.data.solarSystemName) {
        setInputValue(r.data.solarSystemName);
        propsOnChange(r.data.solarSystemName);
      }
    }).catch(() => {});
  }, [globalData.domain, propsOnChange]);

  useEffect(() => {
    const handleClick = (evt: MouseEvent) => {
      const target: any = evt.target;
      const element = document.getElementById(`${props.fieldId}-wrap`);
      if (element && ! element.contains(target)) {
        setOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClick, false);

    return function cleanup() {
      document.removeEventListener('mousedown', handleClick, false);
    };
  }, [props.fieldId]);

  useEffect(() => {
    if (props.fieldId !== startSystemId) {
      return;
    }
    fetchLocation();
  }, [fetchLocation, props.fieldId]);

  useEffect(() => {
    const delayDebounceFn = setTimeout(() => {
      if (searchTerm === '') {
        setSystems([]);
        setOpen(false);
        return;
      }

      (async () => {
        setLoading(true);
        let response = null;
        try {
          response = await axios.get<ResponseSystemNames>(`${globalData.domain}/api/systems/find/${searchTerm}`);
        } catch (error) {
          // do nothing
        }
        setLoading(false);
        setOpen(true);
        if (response) {
          setSystems(response.data.systems);
        } else {
          setSystems([t('app.error')]);
        }
      })();

    }, 300);

    return () => clearTimeout(delayDebounceFn)
    // eslint-disable-next-line
  }, [globalData.domain, searchTerm]); // do not add "t" or a language change can trigger a search

  const onChange = (value: string) => {
    setInputValue(value);
    setSearchTerm(value);
    props.onChange(value);
  };

  const onKeyPress = (evt: React.KeyboardEvent) => {
    if (evt.key === 'Enter' && props.findRoute) {
      props.findRoute();
    }
  };

  const selectSystem = (e: React.MouseEvent) => {
    const system = e.currentTarget.textContent || '';
    setOpen(false);
    setInputValue(system);
    props.onChange(system);

    // @ts-ignore
    textInput.current.focus();
  };

  const clearInput = () => {
    onChange("");
    if (textInput.current !== null) {
      // @ts-ignore
      textInput.current.focus();
    }
  };

  let textInput = useRef(null);

  return (
    <div className={classes.wrap} id={`${props.fieldId}-wrap`}>
      <TextField
        inputRef={textInput}
        variant="outlined"
        className={classes.textField}
        label={props.fieldName}
        id={props.fieldId}
        type="text"
        autoComplete="off"
        value={inputValue}
        onChange={e => onChange(e.target.value)}
        onKeyPress={onKeyPress}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start">
              {props.fieldId === startSystemId &&
                <IconButton
                  className={classes.location}
                  size="small"
                  title={t('systemInput.current-location')}
                  onClick={fetchLocation}
                >
                  <LocationOnOutlinedIcon fontSize="small" />
                </IconButton>
              }
            </InputAdornment>
          ),
          endAdornment: (
            <InputAdornment position="end">
              <CircularProgress color="inherit" size={15} className={!loading ? classes.loadingHidden : ''} />
              {' '}
              <IconButton
                className={classes.clearIndicator}
                style={inputValue.length === 0 ? {visibility: 'hidden'} : {}}
                size="small"
                title={t('systemInput.clear')}
                onClick={clearInput}
              >
                <ClearIcon fontSize="small" />
              </IconButton>
            </InputAdornment>
          )
        }}
      />
      {open &&
        <Box boxShadow={2} className={classes.box}>
          <List>
            {systems.map((value, index) => {
              if (index > 200) {
                return '';
              }
              return (
                <ListItem button key={index} onClick={selectSystem}>{value}</ListItem>
              )
            })}
          </List>
        </Box>
      }
    </div>
  )
})
