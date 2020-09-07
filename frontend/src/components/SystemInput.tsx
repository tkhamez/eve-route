import React, { forwardRef, useContext, useImperativeHandle, useRef } from 'react';
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
import axios from 'axios';
import { GlobalDataContext } from '../GlobalDataContext';
import { ResponseRouteLocation, ResponseSystems } from '../response';

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

  useImperativeHandle(ref, () => ({
    clearInput() {
      onChange('');
    }
  }));

  useEffect(() => {
    if (props.fieldId !== "start-system") {
      return;
    }
    axios.get<ResponseRouteLocation>(`${globalData.domain}/api/route/location`).then(r => {
      if (r.data.code) {
        console.log(r.data.code);
      } else if (r.data.solarSystemName) {
        setInputValue(r.data.solarSystemName);
        props.onChange(r.data.solarSystemName);
      }
    }).catch(() => {
      console.log('Failed to fetch location of character.');
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [globalData.domain, props.fieldId, props.onChange]); // do *not* use "props" here or there will be an endless loop

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
          response = await axios.get<ResponseSystems>(`${globalData.domain}/api/systems/find/${searchTerm}`);
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
  }, [globalData.domain, searchTerm, t]);

  const onChange = (value: string) => {
    setInputValue(value);
    setSearchTerm(value);
    props.onChange(value);
  };

  const selectSystem = (e: React.MouseEvent) => {
    const system = e.currentTarget.textContent || '';
    setOpen(false);
    setInputValue(system);
    props.onChange(system);
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
    <div className={classes.wrap}>
      <TextField
        inputRef={textInput}
        variant="outlined"
        className={classes.textField}
        label={props.fieldName}
        id={props.fieldId}
        type="search"
        autoComplete="off"
        value={inputValue}
        onChange={e => onChange(e.target.value)}
        InputProps={{
          endAdornment: (
            <InputAdornment position="end">
              {loading && false ? <CircularProgress color="inherit" size={15} /> : null}
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
              if (index > 200) { // TODO better way to prevent lag? react-window?
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
